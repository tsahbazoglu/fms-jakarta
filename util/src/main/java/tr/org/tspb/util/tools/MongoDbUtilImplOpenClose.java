package tr.org.tspb.util.tools;

import static tr.org.tspb.constants.ProjectConstants.COLLECTION_NAME;
import static tr.org.tspb.constants.ProjectConstants.DOLAR_SET;
import static tr.org.tspb.constants.ProjectConstants.FORM_DB;
import static tr.org.tspb.constants.ProjectConstants.MONGO_ID;
//
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
//
import tr.org.tspb.constants.exceptions.NullNotExpectedException;
import tr.org.tspb.datamodel.codec.MyBaseRecordCodec;
import tr.org.tspb.datamodel.codec.NullRecordCodec;
import tr.org.tspb.datamodel.dao.MyBaseRecord;
import tr.org.tspb.datamodel.dao.MyField;
import tr.org.tspb.datamodel.dao.MyFile;
import tr.org.tspb.datamodel.dao.FmsFieldItems;
import tr.org.tspb.datamodel.dao.MyLookup;
import tr.org.tspb.datamodel.dao.refs.PlainRecord;
import tr.org.tspb.datamodel.pojo.RoleMap;
import tr.org.tspb.datamodel.dao.FmsFile;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.dao.MyFileNoContent;
import tr.org.tspb.datamodel.dao.TagEvent;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class MongoDbUtilImplOpenClose implements MongoDbUtilIntr {

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

    public MongoDbUtilImplOpenClose(String mongoAdminUser, String mongoAdminPswd,
            ServerAddress serverAddress) {
        this.mongoAdminUser = mongoAdminUser;
        this.mongoAdminPswd = mongoAdminPswd;
        this.serverAddress = serverAddress;
        this.mongoClient = createClient();
        this.mongoClientUser = MongoClients.create(
                MongoClientSettings.builder().
                        applyConnectionString(new ConnectionString(
                                "mongodb://" + host_users + ":" + port + "/?replicaSet=rs0")).
                        build());

    }

    @Override
    public Document findUserOne(String username) {
        Document document = mongoClientUser.getDatabase("uysdb").
                getCollection("common").
                find(Filters.eq("ldapUID", username)).
                first();
        return document;
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

    private static void close(MongoClient mongoClient) {
        if (mongoClient != null) {
            mongoClient.close();
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

    public void dropTable(String UYSDB, String tbbBankCollection) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void deleteOne(String database, String collectionName,
            Document document) {
        MongoClient mongoClient = createClient();
        mongoClient.getDatabase(database).
                getCollection(collectionName).
                deleteOne(document);
        close(mongoClient);
    }

    public List<Document> aggregate(String db, String table,
            List<Document> aggregateList) {

        MongoClient mongoClient = createClient();

        AggregateIterable<Document> output = mongoClient.getDatabase(db).
                getCollection(table).
                aggregate(aggregateList);

        List<Document> list = new ArrayList<>();//188788 record heap space
        for (Document doc : output) {
            list.add(doc);
        }

        close(mongoClient);
        return list;

    }

    public MongoCollection getCollection(String db, String collectionName,
            Document indexObject, boolean unique) {

        MongoClient mongoClient = createClient();

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

    public void createIndex(FmsFieldItems myItems) {
        createIndex(myItems.getDb(), myItems.getTable(), myItems.getSort());
    }

    public void createIndex(FmsForm myForm, Document indexObject) {
        createIndex(myForm.getDb(), myForm.getTable(), indexObject);
    }

    public void createIndex(String db, String collectionName,
            Document indexObject) {
        MongoClient mongoClient = createClient();
        mongoClient.getDatabase(db).
                getCollection(collectionName).
                createIndex(indexObject);
        close(mongoClient);
    }

    public void createIndexUnique(FmsForm myForm, Document indexObject) {
        createIndexUnique(myForm.getDb(), myForm.getTable(), indexObject);
    }

    public void createIndexUnique(String db, String collectionName,
            Document indexObject) {
        MongoClient mongoClient = createClient();
        mongoClient.getDatabase(db).
                getCollection(collectionName).
                createIndex(indexObject, new IndexOptions().unique(true));
        close(mongoClient);
    }

    @Override
    public void remove(String db, String collectionName,
            Map<String, Object> searchMap) throws RuntimeException {
        MongoClient mongoClient = createClient();
        mongoClient.getDatabase(db).
                getCollection(collectionName, null).
                deleteMany(new Document(searchMap));
        close(mongoClient);
    }

    @Override
    public GridFSBucket createGridFSConnection(String database) {
        MongoDatabase db = mongoClient.getDatabase(database);
        return GridFSBuckets.create(db);
        // return GridFSBuckets.create(db, ASSOCIATED_FILES);
    }

    @Override
    public void removeFile(String dbName, Bson filter) throws RuntimeException {
        MongoClient mongoClient = createClient();

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

        close(mongoClient);
    }

    public void removeFile(String dbName, ObjectId objectId) throws
            RuntimeException {
        MongoClient mongoClient = createClient();

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

        close(mongoClient);
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

    public MyFile findFileAsMyFile(String db, ObjectId objectId) throws
            IOException {
        MongoClient mongoClient = createClient();
        GridFSBucket gridFS = createGridFSConnection(db);

        MyFile myFile = new MyFile(gridFS, gridFS.find(Filters.eq("_id",
                objectId)).
                first())
                .withBytes();

        close(mongoClient);
        return myFile;
    }

    @Override
    public void copyFiles(String fromDbName, String toDbName,
            Bson fromSearch)
            throws IOException {

        MongoClient mongoClient = createClient();

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
        close(mongoClient);

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

        MongoClient mongoClient = createClient();

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
        close(mongoClient);
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
                    FmsFieldItems itemsDbo = field.getItemsAsMyItems();
                    if (itemsDbo != null) {
                        String myDb = (String) itemsDbo.getDb();
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

    public boolean insertIntoMongo(String db, String collectionName,
            List<Map> mongoListOfMap) {

        MongoClient mongoClient = createClient();
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
        close(mongoClient);
        return true;//writeResult == null || writeResult.getError() == null;
    }

    public List<Document> findAll(String db, String collectionName) {
        MongoClient mongoClient = createClient();
        List<Document> list = new ArrayList<>();//188788 record heap space
        FindIterable<Document> cursor = mongoClient.getDatabase(db).
                getCollection(collectionName).
                find();
        for (Document doc : cursor) {
            list.add(doc);
        }
        close(mongoClient);
        return list;
    }

    public void drop(String db, String collectionName) {
        MongoClient mongoClient = createClient();
        mongoClient.getDatabase(db).
                getCollection(collectionName).
                drop();
        close(mongoClient);
    }

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
                return runCommand(trigger.getDb(), function, projectSpaceMap,
                        roles);
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

    public Document findOne(String database, String collection, Bson filter) {
        MongoClient mongoClient = createClient();
        Document document = mongoClient.getDatabase(database).
                getCollection(collection).
                find(filter).
                first();
        close(mongoClient);
        return document;
    }

    public Document findOneWithProjection(String database, String collection,
            Bson filter, Bson projection) {
        MongoClient mongoClient = createClient();
        try {
            Document document = mongoClient.getDatabase(database).
                    getCollection(collection).
                    find(filter).
                    projection(projection).
                    first();
            return document;
        } catch (Exception ex) {
            throw ex;
        } finally {
            close(mongoClient);
        }
    }

    public boolean runActionAsDbTableFilterResult(Document actionDoc,
            RoleMap roleMap, Map filter) {
        String detectedRole = "none";

        for (String role : actionDoc.keySet()) {
            if (roleMap.isUserInRole(role)) {
                detectedRole = role;
            }
        }

        Document def = actionDoc.get(detectedRole, Document.class);

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
                if (subQuery.containsKey("db")) {
                    Document subResult = findOneWithProjection(subQuery.
                            getString("db"),
                            subQuery.getString(FmsFieldItems.ITEM_TABLE),
                            expandQuery(subQuery.get("query", Document.class),
                                    filter),
                            new Document(subQuery.getString("projection"), true));
                    query.put(key, subResult.get(subQuery.
                            getString("projection")));
                }
            }
        }
        return query;
    }

    @Override
    public Document runCommand(String dbName, String lookupCode, Object... args) {

        List<Object> reviewedArgs = Arrays.stream(args).
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

        String targetCollectionName = pipelineMeta.getString("collection");
        List<Document> pipelineStages = pipelineMeta.getList("list",
                Document.class);

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

    private Object normalizeArgument(Object obj) {
        if (obj instanceof PlainRecord pr) {
            return pr.getObjectId();
        }
        if (obj instanceof Map<?, ?> map) {
            Document doc = new Document((Map<String, Object>) map);
            doc.replaceAll((k, v) -> (v instanceof PlainRecord vpr) ? vpr.
                    getObjectId() : v);
            return doc;
        }
        return obj;
    }

    public void updateOne(String database, String collection, Bson filter,
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

        MongoClient mongoClient = createClient();
        try {
            mongoClient.getDatabase(database).
                    getCollection(collection).
                    updateOne(filter, new Document(DOLAR_SET, record));
        } catch (Exception ex) {
            throw ex;
        } finally {
            close(mongoClient);
        }
    }

    public void updateMany(FmsForm myForm, Bson filter, Document record) {
        updateMany(myForm.getDb(), myForm.getTable(), filter, record);
    }

    @Override
    public void updateMany(FmsForm myForm, Bson filter, Document record,
            UpdateOptions uo) {
        updateMany(myForm.getDb(), myForm.getTable(), filter, record, uo);
    }

    public void updateMany(String database, String collection, Bson filter,
            Document record, UpdateOptions uo) {
        MongoClient mongoClient = createClient();
        mongoClient.getDatabase(database).
                getCollection(collection).
                updateMany(filter, new Document(DOLAR_SET, record), uo);
        close(mongoClient);
    }

    public void updateMany(String database, String collection, Bson filter,
            Document record) {
        MongoClient mongoClient = createClient();
        mongoClient.getDatabase(database).
                getCollection(collection).
                updateMany(filter, new Document(DOLAR_SET, record));
        close(mongoClient);
    }

    public List<DocumentRecursive> findListAsName(String myFormDb,
            String myFormTable, FmsForm myForm, Document searcheDBObject,
            Integer limit) {
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
                MyField field = myForm.getField(key);
                if (field != null && field.getItemsAsMyItems() != null) {
                    String myDb = field.getItemsAsMyItems().
                            getDb();
                    if (myDb == null) {
                        myDb = myForm.getDb();
                    }
                    String myColl = field.getRefCollection() == null ? field.
                            getItemsAsMyItems().
                            getTable() : field.getRefCollection();
                    def.put(FORM_DB, myDb == null ? myForm.getDb() : myDb);
                    def.put(COLLECTION_NAME, myColl);
                    def.put(MONGO_ID, keyValue);
                    manualDbRefs.put(key, def);
                } else {
                    document.remove(key);
                }
            }
        }
    }

    @Override
    public void insertOne(String database, String collection, Document record) {
        MongoClient mongoClient = createClient();
        mongoClient.getDatabase(database).
                getCollection(collection).
                insertOne(record);
        close(mongoClient);
    }

    @Override
    public void deleteMany(String database, String collection, Document document) {
        MongoClient mongoClient = createClient();
        mongoClient.getDatabase(database).
                getCollection(collection).
                deleteMany(document);
        close(mongoClient);
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

    public List<MyFile> findFilesAsMyFile(String db, DBObject filter) throws
            IOException {
        MongoClient mongoClient = createClient();

        List<MyFile> listOfMyFile = new ArrayList<>();

        close(mongoClient);
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
        MongoClient mongoClient = createClient();
        long count = mongoClient.getDatabase(database).
                getCollection(table).
                countDocuments(relativeQuery);
        close(mongoClient);
        return count;
    }

    @Override
    public List<Document> find(String database, String collection) {
        MongoClient mongoClient = createClient();
        FindIterable<Document> list = mongoClient.getDatabase(database).
                getCollection(collection).
                find();
        List<Document> listOut = new ArrayList<>();
        for (Document document : list) {
            listOut.add(document);
        }
        close(mongoClient);
        return listOut;
    }

    public List<Document> find(String database, String collection, Bson filter) {
        MongoClient mongoClient = createClient();
        FindIterable<Document> list = mongoClient.getDatabase(database).
                getCollection(collection).
                find(filter);
        List<Document> listOut = new ArrayList<>();
        for (Document document : list) {
            listOut.add(document);
        }
        close(mongoClient);
        return listOut;
    }

    public List<Document> findWithProjection(String database, String collection,
            Bson filter, Bson projection) {
        MongoClient mongoClient = createClient();
        FindIterable<Document> list = mongoClient.getDatabase(database).
                getCollection(collection).
                find(filter).
                projection(projection);
        List<Document> listOut = new ArrayList<>();
        for (Document document : list) {
            listOut.add(document);
        }
        close(mongoClient);
        return listOut;
    }

    public List<Document> findWithProjection(String database, String collection,
            Bson filter, Bson sort, Number limit, Bson projection) {
        MongoClient mongoClient = createClient();
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
        close(mongoClient);
        return listOut;
    }

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
        MongoClient mongoClient = createClient();
        FindIterable<Document> list = mongoClient.getDatabase(database).
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
        close(mongoClient);
        return listOut;
    }

    public List<Document> findSkipLimit(String database, String collection,
            Bson filter, Number skip, Number limit) {
        MongoClient mongoClient = createClient();
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
        close(mongoClient);
        return listOut;
    }

    @Override
    public List<Document> createCursor(String myFormDb, String myFormTable,
            Document searcheDBObject, Integer limit) {
        MongoClient mongoClient = createClient();
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
        close(mongoClient);
        return listOut;
    }

    @Override
    public List<Document> createCursor(String myFormDb, String myFormTable,
            Bson searcheDBObject, Integer limit) {
        MongoClient mongoClient = createClient();
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
        close(mongoClient);
        return listOut;
    }

    public Document replaceToDollar(Document document) {
        if (document == null) {
            return null;
        }
        return new RecurcivlyReplaceableDocument(document).replaceToDollar();
    }

    @Override
    public Document cacheAndGet(Map<String, Object> map) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MyFile findFileAsMyFileInputStream(String db, ObjectId objectId)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updatePush(String database, String collection, Bson filter,
            Document record) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void upsertOne(String database, String collection, Bson filter,
            Document record) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
