package tr.org.tspb.util.tools;

import static tr.org.tspb.constants.ProjectConstants.COLLECTION_NAME;
import static tr.org.tspb.constants.ProjectConstants.DOLAR_SET;
import static tr.org.tspb.constants.ProjectConstants.FORM_DB;
import static tr.org.tspb.constants.ProjectConstants.MONGO_ID;
import static tr.org.tspb.constants.ProjectConstants.NAME;
import static tr.org.tspb.constants.ProjectConstants.DOLAR_UNSET;
import static tr.org.tspb.constants.ProjectConstants.RETVAL;

//
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
//
import jakarta.inject.Inject;
//
import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.DBObject;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
//
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.BsonObjectId;
import org.bson.BsonValue;
//
import org.slf4j.Logger;
//
import tr.org.tspb.datamodel.codec.MyBaseRecordCodec;
import tr.org.tspb.datamodel.codec.NullRecordCodec;
import tr.org.tspb.constants.ProjectConstants;
import tr.org.tspb.constants.exceptions.NullNotExpectedException;
import tr.org.tspb.datamodel.dao.MyBaseRecord;
import tr.org.tspb.datamodel.dao.MyField;
import tr.org.tspb.datamodel.dao.MyFile;
import tr.org.tspb.datamodel.dao.MyFileNoContent;
import tr.org.tspb.datamodel.dao.MyItems;
import tr.org.tspb.datamodel.dao.MyLookup;
import tr.org.tspb.datamodel.dao.refs.PlainRecord;
import tr.org.tspb.datamodel.pojo.RoleMap;
import tr.org.tspb.datamodel.dao.FmsFile;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.dao.MyMap;
import tr.org.tspb.datamodel.dao.TagEvent;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class MongoDbUtilImplKeepOpen implements MongoDbUtilIntr {

    @Inject
    private Logger logger;

    public static final String RW_REFERANS = "referans";
    public static final String RW_REPORT = "report";
    public static final String RW_SORT = "sort";
    public static final String RW_USERS = "users";

    public String mongoAdminUser = "to be set on server jndi prop";
    public String mongoAdminPswd = "to be set on server jndi prop";
    public ServerAddress serverAddress = new ServerAddress();
    private final String host = "mongodb";
    private final String host_users = "mongodb";// "localhost","uys.tspb.org.tr";
    private final int port = 27017;
    private final MongoClient mongoClient;
    private final MongoClient mongoClientUser;

    private final Map<String, Document> MAP_OF_RECORD = new HashMap<>();

    public MongoDbUtilImplKeepOpen(String mongoAdminUser, String mongoAdminPswd,
            ServerAddress serverAddress) {
        this.mongoAdminUser = mongoAdminUser;
        this.mongoAdminPswd = mongoAdminPswd;
        this.serverAddress = serverAddress;
        this.mongoClient = createClient();
        //
        this.mongoClientUser = MongoClients.create(
                MongoClientSettings.builder().
                        applyConnectionString(new ConnectionString(
                                "mongodb://" + host_users + ":" + port + "/?replicaSet=rs0")).
                        build());

    }

    private MongoClientSettings getSettings() {
        CodecRegistry defaultRegistry = MongoClientSettings.
                getDefaultCodecRegistry();
        CodecRegistry customRegistry = CodecRegistries.fromCodecs(
                new MyBaseRecordCodec(),
                new NullRecordCodec()
        );
        CodecRegistry allInOne = CodecRegistries.fromRegistries(defaultRegistry,
                customRegistry);

        if (false) {
            MongoCredential credential = MongoCredential.createCredential(
                    mongoAdminUser,
                    "admin",
                    mongoAdminPswd.toCharArray()
            );
            return MongoClientSettings.builder().
                    applyConnectionString(new ConnectionString(
                            "mongodb://" + host + ":" + port + "/?replicaSet=rs0")).
                    credential(credential).
                    writeConcern(WriteConcern.JOURNALED).
                    codecRegistry(allInOne).
                    build();
        } else {
            return MongoClientSettings.builder().
                    applyConnectionString(new ConnectionString(
                            "mongodb://" + host + ":" + port + "/?replicaSet=rs0")).
                    writeConcern(WriteConcern.JOURNALED).
                    codecRegistry(allInOne) // .applyToConnectionPoolSettings(builder -> builder.maxWaitTime(10, TimeUnit.SECONDS))
                    .
                    build();
        }

    }

    private MongoClient createClient() {
        return MongoClients.create(getSettings());
    }

    private static void close(MongoClient client) {
        if (client != null) {
            client.close();
        }
    }

    private static boolean isThereTable(MongoClient mongoClient, String db,
            String collectionName) {
        boolean result = false;
        MongoIterable<String> collection = mongoClient.getDatabase(db).
                listCollectionNames();
        for (String s : collection) {
            if (s.equals(collectionName)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public void dropTable(String UYSDB, String tbbBankCollection) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void remove(String db, String collectionName,
            Map<String, Object> searchMap)
            throws RuntimeException {
        mongoClient.getDatabase(db).
                getCollection(collectionName, null).
                deleteMany(new Document(searchMap));
    }

    @Override
    public void deleteOne(String database, String collectionName,
            Document document) {
        mongoClient.getDatabase(database).
                getCollection(collectionName).
                deleteOne(document);
        // crlear cache
        ObjectId recordId = document.getObjectId("_id");
        if (recordId != null) {
            deleteCacheOne(database, collectionName, recordId);
        }
    }

    @Override
    public void deleteMany(String database, String collection, Document document) {
        mongoClient.getDatabase(database).
                getCollection(collection).
                deleteMany(document);
        // crlear cache
        ObjectId recordId = document.getObjectId("_id");
        if (recordId != null) {
            deleteCacheOne(database, collection, recordId);
        }
    }

    @Override
    public List<Document> aggregate(String db, String table,
            List<Document> aggregateList) {

        AggregateIterable<Document> output = mongoClient.getDatabase(db).
                getCollection(table).
                aggregate(aggregateList);

        List<Document> list = new ArrayList<>();//188788 record heap space
        for (Document doc : output) {
            list.add(doc);
        }

        return list;

    }

    @Override
    public MongoCollection getCollection(String db, String collectionName,
            Document indexObject, boolean unique) {

        MongoCollection dbCollection;

        if (!isThereTable(mongoClient, db, collectionName)) {
            mongoClient.getDatabase(db).
                    createCollection(collectionName);
        }

        dbCollection = mongoClient.getDatabase(db).
                getCollection(collectionName);

        if (indexObject != null) {
            Document index = new Document();
            for (String key : indexObject.keySet()) {
                index.put(key, 1);
            }
            IndexOptions indexOptions = new IndexOptions().unique(true);
            dbCollection.createIndex(index, indexOptions);
        }
        return dbCollection;
    }

    public void createIndex(MyItems myItems) {
        createIndex(myItems.getDb(), myItems.getTable(), myItems.getSort());
    }

    public void createIndex(FmsForm myForm, Document indexObject) {
        createIndex(myForm.getDb(), myForm.getTable(), indexObject);
    }

    public void createIndex(String db, String collectionName,
            Document indexObject) {
        ListIndexesIterable<Document> indexes = mongoClient.getDatabase(db).
                getCollection(collectionName).
                listIndexes();

        // there is no predefined function on mongodb driver to prevent 
        // re-create of an existing index with defferent index options like unique
        // so we proivde it with custom coding
        boolean shouldCreate = true;
        for (Document index : indexes) {
            shouldCreate = shouldCreate && compareIndexes(index, indexObject);
        }

        if (shouldCreate) {
            mongoClient.getDatabase(db).
                    getCollection(collectionName).
                    createIndex(indexObject);
        }
    }

    private boolean compareIndexes(Document index1, Document index2) {

        if (index1.isEmpty()) {
            return false;
        }

        Set<String> index1_key_keys = ((Map) index1.get("key")).keySet();
        Set<String> index2_keys = index2.keySet();

        return index1_key_keys.retainAll(index2_keys);

    }

    @Override
    public void createIndexUnique(FmsForm myForm, Document indexObject) {
        createIndexUnique(myForm.getDb(), myForm.getTable(), indexObject);
    }

    @Override
    public void createIndexUnique(String db, String collectionName,
            Document indexObject) {
        mongoClient.getDatabase(db).
                getCollection(collectionName).
                createIndex(indexObject, new IndexOptions().unique(true));
    }

    @Override
    public void copyFiles(String fromDbName, String toDbName,
            Bson fromSearch)
            throws IOException {

        MongoDatabase fromDb = mongoClient.getDatabase(fromDbName);
        MongoDatabase toDb = mongoClient.getDatabase(toDbName);

        GridFSBucket sourceBucket = GridFSBuckets.create(fromDb);
        GridFSBucket targetBucket = GridFSBuckets.create(toDb);

        List<GridFSFile> files = sourceBucket.find(fromSearch).
                into(new ArrayList<>());

        for (GridFSFile fileIonUploadedFile : files) {
            String sha256;

            try (InputStream hashStream = sourceBucket.openDownloadStream(
                    fileIonUploadedFile.getObjectId())) {
                sha256 = DigestUtils.sha256Hex(hashStream);
            }

            // 4. Prepare Metadata
            Document oldMetadata = fileIonUploadedFile.getMetadata();
            Document newMetadata = (oldMetadata != null) ? new Document(
                    oldMetadata) : new Document();
            newMetadata.append("sha256", sha256);

            GridFSUploadOptions uploadOptions = new GridFSUploadOptions()
                    .metadata(newMetadata);

            // 5. Upload to target (Open a fresh stream for the actual data transfer)
            try (InputStream uploadStream = sourceBucket.openDownloadStream(
                    fileIonUploadedFile.getObjectId())) {
                targetBucket.uploadFromStream(
                        fileIonUploadedFile.getFilename(),
                        uploadStream,
                        uploadOptions
                );
            }
        }
    }

    @Override
    public void removeFile(String dbName, Bson filter) throws
            RuntimeException {
        // 1. Get the bucket for the specific database
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.
                getDatabase(dbName));

        // 2. Find all files matching the filter and delete them one by one
        gridFSBucket.find(filter).
                forEach(new Consumer<GridFSFile>() {
                    @Override
                    public void accept(GridFSFile gridFSFile) {
                        gridFSBucket.delete(gridFSFile.getObjectId());
                    }
                });
    }

    @Override
    public void removeFile(String dbName, ObjectId objectId) throws
            RuntimeException {
        // 1. Get the bucket for the database
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.
                getDatabase(dbName));

        // 2. Delete directly by ObjectId
        try {
            gridFSBucket.delete(objectId);
        } catch (com.mongodb.MongoGridFSException e) {
            // This exception occurs if the file ID is not found
            throw new RuntimeException(
                    "Failed to delete file with ID: " + objectId, e);
        }
    }

    @Override
    public GridFSFile findFile(String dbName, ObjectId objectId) throws
            RuntimeException {
        // 1. Get the bucket for the database
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.
                getDatabase(dbName));

        // 2. Find the file metadata by ID
        // .find() returns an iterable, so we use .first() to get the specific file
        GridFSFile file = gridFSBucket.find(Filters.eq("_id", objectId)).
                first();

        return file;
    }

    @Override
    public GridFSFile findFile(String dbName, Bson filter) throws
            RuntimeException {
        // 1. Initialize the bucket for the target database
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.
                getDatabase(dbName));

        // 2. Perform the search. 
        // .find(filter) returns an iterable; .first() mimics the old 'findOne' behavior.
        return gridFSBucket.find(filter).
                first();
    }

    @Override
    public List<Map<String, Object>> find(FmsForm myForm, String collectionName,
            Map<String, Object> searchMap,
            Map<String, Object> returnMap,
            int skip,
            int limit,
            Map<String, Object> sortMap,
            String searchPrefix) throws NullNotExpectedException {

        List<Map<String, Object>> listMaps = new ArrayList<>();

        if (!isThereTable(mongoClient, myForm.getDb(), collectionName)) {
            return listMaps;//Collections.emptyList();
        }

        Document returnObject = returnMap == null ? new Document() : new Document(
                returnMap);
        Document searchObject = searchMap == null ? new Document() : new Document(
                searchMap);

        FindIterable<Document> dbCursor = mongoClient.
                getDatabase(myForm.getDb()).
                getCollection(collectionName).
                find(searchObject);

        if (sortMap != null && !sortMap.isEmpty()) {
            dbCursor.sort(new Document(sortMap));
        }

        dbCursor.skip(skip).
                limit(limit);

        for (Document doc : dbCursor) {
            listMaps.add(wrapIt(myForm, doc));
        }
        return listMaps;
    }

    public DocumentRecursive wrapIt(FmsForm myForm, Document dBObject) throws
            NullNotExpectedException {
        Map manualDbRefs = new HashMap();

        for (String key : dBObject.keySet()) {
            if (dBObject.get(key) instanceof ObjectId && !MONGO_ID.equals(key)) {
                Map def = new HashMap();
                MyField field = myForm.getField(key);

                if (field != null) {
                    MyItems itemsDbo = field.getItemsAsMyItems();
                    if (itemsDbo != null) {
                        String myDb = itemsDbo.getDb();
                        def.put(FORM_DB, myDb == null ? myForm.getDb() : myDb);
                        def.put(COLLECTION_NAME, itemsDbo.getTable());
                        def.put(MONGO_ID, dBObject.get(key));
                        manualDbRefs.put(key, def);
                    } else {
                        logger.error("items is null : {}", key);
                    }
                }
            }
        }
        return new DocumentRecursive(dBObject, manualDbRefs, this);
    }

    @Override
    public boolean insertIntoMongo(String db, String collectionName,
            List<Map> mongoListOfMap) {

        List<Document> list = new ArrayList<>();
        for (Map map : mongoListOfMap) {
            list.add(new Document(map));
            if (list.size() == 50000) {//prevent heap space error on 136063 record count
                mongoClient.getDatabase(db).
                        getCollection(collectionName).
                        insertMany(list);
                list = new ArrayList<>();
            }
        }
        mongoClient.getDatabase(db).
                getCollection(collectionName).
                insertMany(list);
        return true;//writeResult == null || writeResult.getError() == null;
    }

    public List<Document> findAll(String db, String collectionName) {
        List<Document> list = new ArrayList<>();//188788 record heap space
        FindIterable<Document> cursor = mongoClient.getDatabase(db).
                getCollection(collectionName).
                find();
        for (Document doc : cursor) {
            list.add(doc);
        }
        return list;
    }

    @Override
    public void drop(String db, String collectionName) {
        mongoClient.getDatabase(db).
                getCollection(collectionName).
                drop();
    }

    @Override
    public Object getValue(Map dbObject, String dottedString) {
        String path[] = dottedString.split("[.]");
        Object value = null;
        for (String key : path) {
            if (value instanceof Map) {
                value = ((Document) value).get(key);
            } else {
                value = dbObject.get(key);
            }
        }
        return value;
    }

    @Override
    public Document trigger(Document projectSpaceMap, TagEvent trigger,
            List roles) {
        if (trigger != null) {
            String function = trigger.getOp();

            if (function != null) {
                final Document command = new Document();
                //command.put("$eval", String.format(replaceFuncCode(function.getCode()), replaceParams(projectSpaceMap)));
                Document result = runCommand(trigger.getDb(), function,
                        projectSpaceMap, roles);

                Object returnValue = result.get(RETVAL);

                if (returnValue instanceof Document) {
                    return (Document) returnValue;
                }
            }
        }
        return new Document();

    }

    public List<String> replaceParams(Object... objects) {

        List<String> params = new ArrayList<>();

        for (Object obj : objects) {
            params.add(obj.toString());
        }

        return params;

    }

    public String replaceFuncCode(String code) {

        int start = code.indexOf("(");
        int end = code.indexOf(")");
        int paramCount = code.substring(start, end).
                split(",").length;

        StringBuilder sb = new StringBuilder();

        sb.append(code.substring(0, start));

        for (int i = 0; i < paramCount; i++) {
            sb.append("%s");
            if (i < paramCount) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    @Override
    public Document findUserOne(String username) {
        Document document = mongoClientUser.getDatabase("uysdb").
                getCollection("common").
                find(Filters.eq("ldapUID", username)).
                first();
        return document;
    }

    @Override
    public Document findOne(String database, String collection, Bson filter) {
        Document document = mongoClient.getDatabase(database).
                getCollection(collection).
                find(filter).
                first();
        return document;
    }

    @Override
    public Document findOneWithProjection(String database, String collection,
            Bson filter, Bson projection) {
        try {
            Document document = mongoClient.getDatabase(database).
                    getCollection(collection).
                    find(filter).
                    projection(projection).
                    first();
            return document;
        } catch (Exception ex) {
            throw ex;
        }
    }

    @Override
    public boolean runActionAsDbTableFilterResult(Document actionDoc,
            RoleMap roleMap, Map filter) {
        String detectedRole = "none";

        for (String role : actionDoc.keySet()) {
            if (roleMap.isUserInRole(role) || roleMap.isUserInRole(role.
                    replaceAll("role-", ""))) {
                detectedRole = role;
            }
        }

        if (actionDoc.get(detectedRole) instanceof Boolean) {
            return actionDoc.get(detectedRole, Boolean.class);
        }

        if (actionDoc.get("role-".concat(detectedRole)) instanceof Boolean) {
            return actionDoc.get("role-".concat(detectedRole), Boolean.class);
        }

        Document def = actionDoc.get("role-".concat(detectedRole),
                Document.class);

        Document query = def.get("query", Document.class);

        query = expandQuery(query, filter);

        Document doc = findOneWithProjection(def.getString("db"), def.getString(
                "table"), query, new Document("_id", true));

        String check = def.getString("check");

        switch (check) {
            case "result>0":
                return doc != null;

        }
        return true;

    }

    public Document expandQuery(Document query, Map filter) {
        query = replaceToDollar(query);
        for (String key : query.keySet()) {
            if (query.get(key) instanceof String) {
                String value = query.get(key).
                        toString();
                if (value.startsWith("${filter.") && value.endsWith("}")) {
                    String filterKey = value.substring(9, value.length() - 1);
                    query.put(key, filter.get(filterKey));
                }
            } else if (query.get(key) instanceof Document) {
                Document subQuery = query.get(key, Document.class);
                if (subQuery.containsKey(MyItems.ITEM_DB)) {
                    Document q = expandQuery(subQuery.get("query",
                            Document.class), filter);
                    Document subResult = findOneWithProjection(
                            subQuery.getString(MyItems.ITEM_DB),
                            subQuery.getString(MyItems.ITEM_TABLE),
                            q,
                            new Document(subQuery.getString("projection"), true));
                    query.put(key, subResult == null ? null : subResult.get(
                            subQuery.getString("projection")));
                }
            }
        }
        return query;
    }

    @Override
    public Document runCommand(String dbName, String lookupCode, Object... args) {

        List<Document> reviewedArgs = Arrays.stream(args).
                map(this::normalizeArgument).
                toList();

        MongoDatabase database = mongoClient.getDatabase(dbName);

        Document pipelineMeta = database.getCollection("pipline").
                find(Filters.eq("code", lookupCode)).
                hint(new Document("code", 1)).
                first();

        if (pipelineMeta == null) {
            throw new NoSuchElementException(
                    "Pipeline definition not found for: " + lookupCode);
        }

        String strategy = pipelineMeta.getString("strategy");

        List<Document> pipelineStages = pipelineMeta.getList("list",
                Document.class);

        if ("AGGREGATE_VIRTUAL".equals(strategy)) {

            Document finalDoc = new Document();
            reviewedArgs.forEach(finalDoc::putAll);

            return executeVirtualAggregate(database, pipelineStages,
                    finalDoc);

        } else {
            String targetCollectionName = pipelineMeta.getString("collection");

            List<Document> executablePipeline = new ArrayList<>();

            if (!reviewedArgs.isEmpty()) {
                executablePipeline.add(new Document("$addFields", new Document(
                        "_args", reviewedArgs)));
            }
            executablePipeline.addAll(pipelineStages);
            executablePipeline.add(new Document("$limit", 1));

            MongoCollection<Document> collection
                    = database.getCollection(targetCollectionName);

            Document result = collection.aggregate(executablePipeline).
                    maxTime(5, TimeUnit.SECONDS).
                    first();

            return result != null ? result : new Document();
        }

    }

    private Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Document cleanDoc = new Document();
            map.forEach((k, v) -> {
                // Replace $ with # in the key
                String newKey = k.toString().
                        replace("$", "#");
                // Recursively sanitize the value
                cleanDoc.append(newKey, sanitizeValue(v));
            });
            return cleanDoc;
        } else if (value instanceof List<?> list) {
            // Sanitize every element in the list (in case of nested docs)
            return list.stream().
                    map(this::sanitizeValue).
                    toList();
        } else if (value instanceof PlainRecord pr) {
            // Keep your existing logic for PlainRecords
            return pr.getObjectId();
        }
        return value;
    }

    private Document executeVirtualAggregate(MongoDatabase database,
            List<Document> pipelineStages, Document args) {

        List<Document> argAslistOfDocuemnt = new ArrayList<>();
        argAslistOfDocuemnt.add((Document) sanitizeValue(args));

        List<Document> virtualPipeline = new ArrayList<>();

        virtualPipeline.add(new Document("$documents", argAslistOfDocuemnt));

        virtualPipeline.addAll(pipelineStages);

        // 3. Execute on the server
        // We can call this on 'database' directly or any collection handle
        // any valid name works
        Document result = database.
                aggregate(virtualPipeline).
                first();

        if (result != null && result.containsKey("result")) {
            Object obj = result.get("result");
            return new Document(RETVAL, obj);
        }

        return result;

    }

    private Document normalizeArgument(Object obj) {
        if (obj instanceof PlainRecord pr) {
            return new Document("_id", pr.getObjectId());
        }
        if (obj instanceof Map<?, ?> map) {
            Document doc = new Document((Map<String, Object>) map);
            doc.replaceAll((k, v) -> (v instanceof PlainRecord vpr) ? vpr.
                    getObjectId() : v);
            return doc;
        }

        if (obj instanceof Iterable<?> iterable) {
            List<Object> values = new ArrayList<>();
            for (Object item : iterable) {
                // Apply normalization to items inside the list as well
                if (item instanceof PlainRecord pr) {
                    values.add(pr.getObjectId());
                } else {
                    values.add(item);
                }
            }
            return new Document("roles", values);
        }

        return new Document("dummy", "dummy");
    }

    @Override
    public void updatePush(String database, String collection, Bson filter,
            Document record) {

        for (String key : record.keySet()) {
            if (record.get(key) instanceof Object[]) {
                Object[] multiValues = (Object[]) record.get(key);
                List<String> list = new ArrayList<>();
                for (Object obj : multiValues) {
                    list.add(obj.toString());
                }
                record.put(key, list);
            } else if (record.get(key) instanceof MyBaseRecord) {
                record.put(key, ((MyBaseRecord) record.get(key)).getObjectId());
            }
        }

        try {
            mongoClient.getDatabase(database).
                    getCollection(collection).
                    updateOne(filter, new Document(ProjectConstants.DOLAR_PUSH,
                            record));
        } catch (Exception ex) {
            throw ex;
        }

    }

    @Override
    public void upsertOne(String database, String collection, Bson filter,
            Document record) {

        for (String key : record.keySet()) {
            if (record.get(key) instanceof Object[]) {
                Object[] multiValues = (Object[]) record.get(key);
                List<String> list = new ArrayList<>();
                for (Object obj : multiValues) {
                    list.add(obj.toString());
                }
                record.put(key, list);
            } else if (record.get(key) instanceof String) {
                record.put(key, ((String) record.get(key)).replaceAll("<", "").
                        replaceAll(">", ""));
            } else if (record.get(key) instanceof MyBaseRecord) {
                record.put(key, ((MyBaseRecord) record.get(key)).getObjectId());
            }
        }

        try {
            mongoClient.getDatabase(database).
                    getCollection(collection).
                    updateOne(filter, new Document(DOLAR_SET, record),
                            new UpdateOptions().upsert(true));
        } catch (Exception ex) {
            throw ex;
        }
    }

    public void insertOne(String database, String collection, Document record) {

        for (String key : record.keySet()) {
            if (record.get(key) instanceof Object[]) {
                Object[] multiValues = (Object[]) record.get(key);
                List list = new ArrayList<>();
                for (Object obj : multiValues) {
                    if (obj instanceof ObjectId) {
                        list.add(obj);
                    } else {
                        list.add(obj.toString());
                    }
                }
                record.put(key, list);
            } else if (record.get(key) instanceof String) {
                record.put(key, ((String) record.get(key)).replaceAll("<", "").
                        replaceAll(">", ""));
            }
        }

        List<MyMap> childs = (List<MyMap>) record.get(MyMap.__CHILDS);
        if (childs != null) {
            for (MyMap child : childs) {
                Document unset = (Document) child.remove(DOLAR_UNSET);
                if (unset != null && !unset.isEmpty()) {
                    for (String key : unset.keySet()) {
                        child.remove(key);
                    }
                }
            }
        }

        Document setUnset = new Document(DOLAR_SET, record);
        if (record.containsKey(DOLAR_UNSET)) {
            setUnset.append(DOLAR_UNSET, record.remove(DOLAR_UNSET));
            if (((Document) setUnset.get(DOLAR_UNSET)).isEmpty()) {
                setUnset.remove(DOLAR_UNSET);
            }
        }

        mongoClient.getDatabase(database).
                getCollection(collection).
                insertOne(record);
    }

    @Override
    public void updateOne(String database, String collection, Bson filter,
            Document record) {

        for (String key : record.keySet()) {
            if (record.get(key) instanceof Object[]) {
                Object[] multiValues = (Object[]) record.get(key);
                List list = new ArrayList<>();
                for (Object obj : multiValues) {
                    if (obj instanceof ObjectId) {
                        list.add(obj);
                    } else {
                        list.add(obj.toString());
                    }
                }
                record.put(key, list);
            } else if (record.get(key) instanceof String) {
                record.put(key, ((String) record.get(key)).replaceAll("<", "").
                        replaceAll(">", ""));
            } else if (record.get(key) instanceof MyBaseRecord) {
                record.put(key, ((MyBaseRecord) record.get(key)).getObjectId());
            }
        }

        List<MyMap> childs = (List<MyMap>) record.get(MyMap.__CHILDS);
        if (childs != null) {
            for (MyMap child : childs) {
                Document unset = (Document) child.remove(DOLAR_UNSET);
                if (unset != null && !unset.isEmpty()) {
                    for (String key : unset.keySet()) {
                        child.remove(key);
                    }
                }
            }
        }

        try {

            Document unset = (Document) record.remove(DOLAR_UNSET);

            Document setUnset = new Document();

            if (unset != null && !unset.isEmpty()) {
                for (String key : unset.keySet()) {
                    record.remove(key);
                }
                setUnset.append(DOLAR_UNSET, unset);

            }
            setUnset.append(DOLAR_SET, record);

            mongoClient.getDatabase(database).
                    getCollection(collection).
                    updateOne(filter, setUnset);

            ObjectId recordId = record.getObjectId("_id");

            if (recordId != null) {
                deleteCacheOne(database, collection, recordId);
            }

        } catch (Exception ex) {
            throw ex;
        }
    }

    public void updateMany(FmsForm myForm, Bson filter, Document record,
            UpdateOptions uo) {
        updateMany(myForm.getDb(), myForm.getTable(), filter, record, uo);
    }

    public void updateMany(FmsForm myForm, Bson filter, Document record) {
        updateMany(myForm.getDb(), myForm.getTable(), filter, record);
    }

    public void updateMany(String database, String collection,
            Bson filter, Document record, UpdateOptions uo) {
        mongoClient.getDatabase(database).
                getCollection(collection).
                updateMany(filter, new Document(DOLAR_SET, record), uo);
    }

    public void updateMany(String database, String collection,
            Bson filter, Document record) {
        mongoClient.getDatabase(database).
                getCollection(collection).
                updateMany(filter, new Document(DOLAR_SET, record));
    }

    public List<DocumentRecursive> findListAsName(String myFormDb,
            String myFormTable,
            FmsForm myForm, Document searcheDBObject, Integer limit) {
        List<Document> cursor = createCursor(myFormDb, myFormTable,
                searcheDBObject, limit);
        return cursorToListAsName(cursor, myForm);
    }

    private List<DocumentRecursive> cursorToListAsName(List<Document> cursor,
            FmsForm myForm) {
        List<DocumentRecursive> list = new ArrayList<>();

        for (Document document : cursor) {
            Map manualDbRefs = new HashMap();
            armBsonRefs(document, myForm, manualDbRefs);
            list.add(new DocumentRecursive(document, manualDbRefs, this));
        }
        return list;
    }

    private static void armBsonRefs(Document document, FmsForm myForm,
            Map manualDbRefs) {
        Set<String> keySet = new HashSet<>(document.keySet());
        for (String key : keySet) {
            Object keyValue = document.get(key);

            if (keyValue instanceof ObjectId && !MONGO_ID.equals(key)) {
                Map def = new HashMap();
                MyField myField = myForm.getField(key);
                if (myField != null && myField.getItemsAsMyItems() != null) {
                    String myDb = myField.getItemsAsMyItems().
                            getDb();
                    if (myDb == null) {
                        myDb = myForm.getDb();
                    }
                    String myColl = myField.getRefCollection() == null ? myField.
                            getItemsAsMyItems().
                            getTable() : myField.getRefCollection();
                    def.put(FORM_DB, myDb == null ? myForm.getDb() : myDb);
                    def.put(COLLECTION_NAME, myColl);
                    def.put(MONGO_ID, keyValue);
                    def.put("viewKeys", myField.getViewKey() == null ? Arrays.
                            asList(NAME) : myField.getViewKey());
                    manualDbRefs.put(key, def);
                } else {
                    document.remove(key);
                }
            }
        }
    }

    @Override
    public List<GridFSFile> findFiles(String dbName, Bson filter) {
        // 1. Get the bucket for the database
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.
                getDatabase(dbName));

        // 2. Execute the find and collect results into a new ArrayList
        // .into() is the most convenient way to convert the iterable to a List
        return gridFSBucket.find(filter).
                into(new ArrayList<>());
    }

    @Override
    public GridFSBucket createGridFSConnection(String database) {
        MongoDatabase db = mongoClient.getDatabase(database);
        return GridFSBuckets.create(db);
        // return GridFSBuckets.create(db, ASSOCIATED_FILES);
    }

    @Override
    public MyFile findFileAsMyFile(String db, ObjectId objectId) throws
            IOException {
        GridFSBucket gridFS = createGridFSConnection(db);
        MyFile myFile = new MyFile(gridFS, gridFS.find(Filters.eq("_id",
                objectId)).
                first())
                .withBytes();
        return myFile;
    }

    @Override
    public MyFile findFileAsMyFileInputStream(String db, ObjectId objectId)
            throws IOException {
        GridFSBucket gridFS = createGridFSConnection(db);
        MyFile myFile = new MyFile(gridFS, gridFS.find(Filters.eq("_id",
                objectId)).
                first());
        return myFile;
    }

    public List<MyFile> findFilesAsMyFile(String db, DBObject filter)
            throws IOException {

        List<MyFile> listOfMyFile = new ArrayList<>();

        GridFSBucket gridFS = createGridFSConnection(db);

        Collation collation = Collation.builder().
                locale("tr").
                build();

        GridFSFindIterable gridFSFindIterable = gridFS.find(new Document(filter.
                toMap()));

        if (gridFSFindIterable.iterator().
                hasNext()) {
            gridFSFindIterable
                    //                .collation(collation)
                    .forEach(
                            (Consumer<com.mongodb.client.gridfs.model.GridFSFile>) gridFSFile -> {
                                try {
                                    listOfMyFile.add(new MyFile(gridFS,
                                            gridFSFile));
                                } catch (IOException ex) {
                                    java.util.logging.Logger.getLogger(
                                            MongoDbUtilImplKeepOpen.class.
                                                    getName()).
                                            log(Level.SEVERE, null, ex);
                                }
                            });
        }

        return listOfMyFile;
    }

    @Override
    public int countFile(String dbName, Bson filter) {
        MongoDatabase database = mongoClient.getDatabase(dbName);

        // GridFS stores file metadata in the "[bucketName].files" collection.
        // The default bucket name is "fs".
        long count = database.getCollection("fs.files").
                countDocuments(filter);

        return (int) count;
    }

    public List<FmsFile> findFilesAsFmsFileNoContent(String db,
            BasicDBObject basicDBObject, int skip, int limit) {

        GridFSBucket gridFS = createGridFSConnection(db);

        MongoCursor cursor = gridFS.find(basicDBObject).
                skip(skip).
                limit(limit).
                iterator();

        List<FmsFile> listOut = new ArrayList<>();

        while (cursor.hasNext()) {
            GridFSFile file = (GridFSFile) cursor.next();
            try {
                listOut.add(new MyFileNoContent(file));
            } catch (IOException ex) {
                logger.error("error occured", ex);
            }
        }
        return listOut;
    }

    @Override
    public List<MyFile> findFileList(String db, BasicDBObject basicDBObject,
            int skip, int limit) {

        GridFSBucket gridFS = createGridFSConnection(db);

        MongoCursor cursor = gridFS.find(basicDBObject).
                skip(skip).
                limit(limit).
                iterator();

        List<MyFile> listOut = new ArrayList<>();

        while (cursor.hasNext()) {
            GridFSFile file = (GridFSFile) cursor.next();
            try {
                listOut.add(new MyFile(gridFS, file));
            } catch (IOException ex) {
                logger.error("error occured", ex);
            }
        }
        return listOut;
    }

    @Override
    public List<GridFSFile> findFiles(String dbName, String filename) {
        // 1. Get the bucket for the specific database
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.
                getDatabase(dbName));

        // 2. Search explicitly by the filename field
        // .into(new ArrayList<>()) iterates the cursor and fills the list for you
        return gridFSBucket.find(Filters.eq("filename", filename)).
                into(new ArrayList<>());
    }

    @Override
    public ObjectId createFile(String dbName, String filename, File file,
            Document externalMetadata) throws IOException {
        // 1. Get the bucket for the database
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.
                getDatabase(dbName));

        // 2. Setup options (Optional: you can pass metadata here)
        Document finalMetadata = new Document("sourcePath", file.
                getAbsolutePath());
        if (externalMetadata != null) {
            finalMetadata.putAll(externalMetadata);
        }

        // 2. Prepare upload options (equivalent to setting metadata on GridFSInputFile)
        GridFSUploadOptions options = new GridFSUploadOptions()
                .metadata(finalMetadata);

        // 3. Open the file stream and upload
        // The bucket handles creating the chunks and the files collection entry automatically
        try (InputStream inputStream = new FileInputStream(file)) {
            return gridFSBucket.uploadFromStream(filename, inputStream,
                    options);
        }
    }

    @Override
    public ObjectId createFile(String dbName, String filename,
            InputStream inputStream, Document externalMetadata) {
        // 1. Get the bucket
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.
                getDatabase(dbName));

        // 2. Setup options (Optional: you can pass metadata here)
        Document finalMetadata = new Document("upload_type", "streamed");
        if (externalMetadata != null) {
            finalMetadata.putAll(externalMetadata);
        }

        GridFSUploadOptions options = new GridFSUploadOptions()
                .metadata(finalMetadata);

        // 3. Upload and return the new ID
        // Note: The driver does NOT close the inputStream for you; 
        // the caller is usually responsible for closing it.
        return gridFSBucket.uploadFromStream(filename, inputStream, options);
    }

    @Override
    public ObjectId createFile(FmsGridFsUploadRequest gridFsUploadRequest) {
        // 1. Get the bucket
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.
                getDatabase(gridFsUploadRequest.getDbName()));

        // 2. Wrap the byte array in an InputStream
        try (InputStream stream = new ByteArrayInputStream(gridFsUploadRequest.
                getContent())) {

            Document finalMetadata = new Document("type", "buffer_upload");
            if (gridFsUploadRequest.getMetadata() != null) {
                finalMetadata.putAll(gridFsUploadRequest.getMetadata());
            }

            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(gridFsUploadRequest.getChunkSize()).
                    metadata(finalMetadata);

            String fileName = gridFsUploadRequest.getFilename();

            if (gridFsUploadRequest.getId() != null) {

                BsonValue bsonValue = new BsonObjectId(gridFsUploadRequest.
                        getId());

                gridFSBucket.
                        uploadFromStream(bsonValue, fileName,
                                stream, options);
                return gridFsUploadRequest.getId();

            } else {
                return gridFSBucket.uploadFromStream(fileName, stream, options);
            }

        } catch (java.io.IOException e) {
            // Technically ByteArrayInputStream doesn't throw IOException on close,
            // but we catch it to satisfy the try-with-resources requirement.
            throw new RuntimeException("Failed to upload byte array to GridFS",
                    e);
        }
    }

    @Override
    public long count(String database, String table, Bson relativeQuery) {
        long count = mongoClient.getDatabase(database).
                getCollection(table).
                countDocuments(relativeQuery);
        return count;
    }

    @Override
    public List<Document> find(String database, String collection) {
        FindIterable<Document> list = mongoClient.getDatabase(database).
                getCollection(collection).
                find();
        List<Document> listOut = new ArrayList<>();
        for (Document document : list) {
            listOut.add(document);
        }
        return listOut;
    }

    @Override
    public List<Document> find(String database, String collection, Bson filter) {
        FindIterable<Document> list = mongoClient.getDatabase(database).
                getCollection(collection).
                find(filter);
        List<Document> listOut = new ArrayList<>();
        for (Document document : list) {
            listOut.add(document);
        }
        return listOut;
    }

    @Override
    public List<Document> findWithProjection(String database, String collection,
            Bson filter, Bson projection) {
        FindIterable<Document> list = mongoClient.getDatabase(database).
                getCollection(collection).
                find(filter).
                projection(projection);
        List<Document> listOut = new ArrayList<>();
        for (Document document : list) {
            listOut.add(document);
        }
        return listOut;
    }

    public List<Document> findWithProjection(String database, String collection,
            Bson filter, Bson sort, Number limit, Bson projection) {
        FindIterable<Document> list = mongoClient.getDatabase(database).
                getCollection(collection).
                find(filter).
                projection(projection);

        if (sort != null) {
            list.sort(sort);
        }

        if (limit != null) {
            list.limit(limit.intValue());
        }

        List<Document> listOut = new ArrayList<>();
        for (Document document : list) {
            listOut.add(document);
        }
        return listOut;
    }

    @Override
    public List<Document> findProjectLookup(String database, String collection,
            Bson filter, Bson sort, Number limit, Bson projection,
            MyLookup myLookup, Document resultProjection) {

        List<Document> documents = findWithProjection(database, collection,
                filter, sort, limit, projection);

        if (myLookup == null) {
            return documents;
        }

        for (Document doc : documents) {

            String foreignKey = myLookup.getFk();

            Document foreignDoc = findOneWithProjection(myLookup.getDb(),
                    myLookup.getTable(),
                    Filters.eq("_id", doc.getObjectId(foreignKey)),
                    myLookup.getFp());

            doc.append(foreignKey, foreignDoc);

            if (resultProjection != null) {

                for (Map.Entry resultProjectionEntry : resultProjection.
                        entrySet()) {

                    String type = ((Document) resultProjectionEntry.getValue()).
                            getString("type");
                    List subkey = Arrays.asList(
                            ((Document) resultProjectionEntry.getValue()).
                                    getString("subkey").
                                    split("[.]"));

                    Class clazz = String.class;

                    switch (type) {
                        case "objectid":
                            clazz = ObjectId.class;
                            break;
                        case "string":
                            clazz = String.class;
                            break;
                        case "number":
                            clazz = Number.class;
                            break;
                        default:
                            break;

                    }
                    doc.put(resultProjectionEntry.getKey().
                            toString(), doc.getEmbedded(subkey, clazz));
                }
            }

        }

        return documents;
    }

    public List<Document> find(String database, String collection, Bson filter,
            Bson sort, Number limit) {
        FindIterable<Document> list = mongoClient
                .getDatabase(database).
                getCollection(collection).
                find(filter);

        if (sort != null) {
            list.sort(sort);
        }

        if (limit != null) {
            list.limit(limit.intValue());
        }

        List<Document> listOut = new ArrayList<>();
        for (Document document : list) {
            listOut.add(document);
        }
        return listOut;
    }

    public List<Document> findSkipLimit(String database, String collection,
            Bson filter, Number skip, Number limit) {
        FindIterable<Document> list = mongoClient.getDatabase(database).
                getCollection(collection).
                find(filter);

        if (skip != null) {
            list.skip(skip.intValue());
        }

        if (limit != null) {
            list.limit(limit.intValue());
        }

        List<Document> listOut = new ArrayList<>();
        for (Document document : list) {
            listOut.add(document);
        }
        return listOut;
    }

    @Override
    public List<Document> createCursor(String myFormDb, String myFormTable,
            Document searcheDBObject, Integer limit) {
        FindIterable<Document> cursor = mongoClient.getDatabase(myFormDb).
                getCollection(myFormTable).
                find(searcheDBObject);
        if (limit != null) {
            cursor = cursor.limit(limit);
        }
        List<Document> listOut = new ArrayList<>();
        for (Document document : cursor) {
            listOut.add(document);
        }
        return listOut;
    }

    @Override
    public List<Document> createCursor(String myFormDb, String myFormTable,
            Bson searcheDBObject, Integer limit) {
        FindIterable<Document> cursor = mongoClient.getDatabase(myFormDb).
                getCollection(myFormTable).
                find(searcheDBObject);
        if (limit != null) {
            cursor = cursor.limit(limit);
        }
        List<Document> listOut = new ArrayList<>();
        for (Document document : cursor) {
            listOut.add(document);
        }
        return listOut;
    }

    @Override
    public Document replaceToDollar(Document document) {
        if (document == null) {
            return null;
        }
        return new RecurcivlyReplaceableDocument(document).replaceToDollar();
    }

    public void deleteCacheOne(String db, String collectionName, ObjectId id) {
        MAP_OF_RECORD.remove(createCacheKey(db, collectionName, id));
    }

    public Document cacheAndGet(Map<String, Object> map) {

        String db = (String) map.get("db");
        String collectionName = (String) map.get("collectionName");
        ObjectId id = (ObjectId) map.get("_id");

        String cacheKey = createCacheKey(db, collectionName, id);

        Document doc = MAP_OF_RECORD.get(cacheKey);

        if (doc == null) {
            doc = findOne(db, collectionName, Filters.eq("_id", id));
            MAP_OF_RECORD.put(cacheKey, doc);
        }

        return doc;
    }

    public String createCacheKey(String db, String table, ObjectId objectId) {
        StringBuilder sb = new StringBuilder();
        sb.append(db).
                append(":").
                append(table).
                append(":").
                append(objectId.toString());
        return sb.toString();
    }

}
