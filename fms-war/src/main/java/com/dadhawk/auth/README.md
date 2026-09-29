# dadhawk.com demo: Jakarta EE 10 + GlassFish 7 + OpenLDAP + MongoDB

OpenLDAP = authentication **and roles**: each company is an OU under `ou=companies`, and each role is a `groupOfNames`
in it (`cn=admin,ou=acme,ou=companies,dc=dadhawk,dc=com`). Membership in the group = the user's role in that company.
MongoDB = role -> CRUD permission map (`roles` collection) and invoices.
Login returns a signed JWT (HS256, 1h). Every request sends `Authorization: Bearer <token>`; invoice queries are
filtered by the token's `companyId` and every action checks a permission. No server-side session.

## Run

```bash
docker compose up -d                 # OpenLDAP (dc=dadhawk,dc=com) + MongoDB
                                     # loads ldap/01-users.ldif, then ldap/02-companies.ldif (roles)
mvn clean package                    # needs JDK 17+
# GlassFish 7 (Jakarta EE 10):
asadmin start-domain
asadmin deploy --force target/demo.war
```

Open http://localhost:8080/demo/ for the login page.

Optional env vars: JWT_SECRET (set your own, 32+ chars), LDAP_URL, LDAP_BASE, LDAP_BIND_DN, LDAP_BIND_PW, MONGO_URI
(set before `asadmin start-domain`).

## Users and LDAP roles (password: password123)

| user  | acme   | globex |
|-------|--------|--------|
| alice | admin  |        |
| bob   | viewer |        |
| carol | editor | admin  |

## Try it with curl

```bash
B=http://localhost:8080/demo/api
login() { curl -s -H 'Content-Type: application/json' -d "$1" $B/auth/login | sed -E 's/.*"token":"([^"]+)".*/\1/'; }

A=$(login '{"username":"alice","password":"password123"}')
curl -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d '{"title":"Inv 1","amount":100}' $B/invoices
curl -H "Authorization: Bearer $A" $B/invoices

# bob (viewer): read OK, create -> 403
BOB=$(login '{"username":"bob","password":"password123"}')
curl -i -H "Authorization: Bearer $BOB" -H 'Content-Type: application/json' -d '{"title":"x","amount":1}' $B/invoices

# carol in the other company: sees none of Acme's invoices
C=$(login '{"username":"carol","password":"password123","companyId":"globex"}')
curl -H "Authorization: Bearer $C" $B/invoices
```

## Change a role

Add or remove the user's DN as `member` of a role group, e.g. with ldapmodify or phpLDAPadmin:

```
dn: cn=editor,ou=acme,ou=companies,dc=dadhawk,dc=com
changetype: modify
add: member
member: uid=bob,ou=users,dc=dadhawk,dc=com
```

A new role name needs a matching document in the Mongo `roles` collection (
`{_id:"auditor", permissions:["invoices:read"]}`).
Roles take effect at the next login (the JWT keeps the old role until it expires).

## Before production

Use ldaps:// or StartTLS, a strong JWT_SECRET (or RS256 with a key pair), refresh tokens or short TTLs, hash LDAP
passwords (SSHA), add Mongo auth, run behind HTTPS
and add a compound index on `invoices(companyId, ...)`.

## Container login (Jakarta Security) and getting the company

Besides the JWT login, `/api/whoami` uses the container's own login (HTTP Basic + `LdapIdentityStore`).
After a successful login, `CompanyContext.companyId()` returns the company from LDAP (e.g. `DENEME_GS_1`).

```bash
curl -u ayse@dadhawk.com:password123 -H 'X-Company: DENEME_GS_1' http://localhost:8080/demo/api/whoami
```

If the user belongs to only one company, `X-Company` can be left out. With several, it is required.

## Users in several companies (consultants)

A user can be a member of role groups in any number of companies (e.g. ayse in DENEME_GS_1 and DENEME_KURUM).

- Login picks `companyId` if sent, otherwise the first company alphabetically. The response lists all `companies`.
- `POST /api/auth/switch {"companyId":"DENEME_KURUM"}` with the current Bearer token returns a new token for that
  company.
  LDAP is re-read on every switch, so a removed role stops working at the next switch or login.
- A token is always for exactly one company, so data from two companies is never mixed in one request.
- Several roles in the same company are combined (union of their permissions).
- The container-login endpoint (`/api/whoami`) uses the `X-Company` header for the same purpose.
