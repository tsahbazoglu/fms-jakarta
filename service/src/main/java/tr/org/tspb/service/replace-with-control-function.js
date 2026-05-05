


db.dataBankConstraintFormulas.findOne({relations: "H1"});
db.dataBankConstraintFormulas.findOne({relations: "H2"});
db.dataBankConstraintFormulas.findOne({relations: "H3"});
db.dataBankConstraintFormulas.findOne({relations: "H4"});
db.dataBankConstraintFormulas.findOne({relations: "H5"});


db.dataBankConstraintFormulas.distinct("engine");
db.dataBankConstraintFormulas.distinct("engine");
db.dataBankConstraintFormulas.distinct("engine");
db.dataBankConstraintFormulas.distinct("engine");
db.dataBankConstraintFormulas.distinct("engine");

["jeval", "javascript", "mongodbFunction"]

db.dataBankConstraintFormulas.distinct("engine", {relations: "H1"});
db.dataBankConstraintFormulas.distinct("engine", {relations: "H2"});
db.dataBankConstraintFormulas.distinct("engine", {relations: "H3"});
db.dataBankConstraintFormulas.distinct("engine", {relations: "H4"});
db.dataBankConstraintFormulas.distinct("engine", {relations: "H5"});

db.dataBankConstraintFormulas.findOne({"engine": "mongodbFunction"});


var bulk = db.getSisterDB("uysdb").dataBankConstraintFormulas.initializeUnorderedBulkOp();
db.getSisterDB("uysdb").dataBankConstraintFormulas.find({
    relations: {$in: ["H1", "H2", "H3", "H4", "H5"]}
}).forEach(function (v) {
    bulk.find({_id: v._id}).updateOne({$set: {
            "engine": "mongodbFunction",
            "controlFunction": function (searchObject, roles, crudObject) {
                return {result: true, expression: "under construction"};
            }
        }
    });
});
bulk.execute();



var bulk = db.getSisterDB("uysdb").dataBankConstraintFormulas.initializeUnorderedBulkOp();
db.getSisterDB("uysdb").dataBankConstraintFormulas.find({
    relations: {$in: ["H1", "H2", "H3", "H4", "H5"]}
}).forEach(function (v) {
    bulk.find({_id: v._id}).updateOne({$unset: {
            "engine": true,
            "controlFunction": true
        }
    });
});
bulk.execute();