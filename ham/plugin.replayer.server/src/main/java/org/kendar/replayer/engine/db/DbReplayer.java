package org.kendar.replayer.engine.db;

import org.apache.commons.lang3.ClassUtils;
import org.kendar.janus.cmd.Close;
import org.kendar.janus.cmd.connection.ConnectionConnect;
import org.kendar.janus.cmd.interfaces.*;
import org.kendar.janus.results.JdbcResult;
import org.kendar.janus.results.ObjectResult;
import org.kendar.janus.results.VoidResult;
import org.kendar.janus.serialization.JsonTypedSerializer;
import org.kendar.replayer.storage.CallIndex;
import org.kendar.replayer.engine.ReplayerEngine;
import org.kendar.replayer.storage.ReplayerRow;
import org.kendar.servers.db.HibernateSessionFactory;
import org.kendar.servers.http.Request;
import org.kendar.servers.http.Response;
import org.kendar.utils.ConstantsHeader;
import org.kendar.utils.ConstantsMime;
import org.kendar.utils.LoggerBuilder;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DbReplayer implements ReplayerEngine {
    public ReplayerEngine create(LoggerBuilder loggerBuilder){
        return new DbReplayer(sessionFactory,loggerBuilder);
    }

    public DbReplayer(HibernateSessionFactory sessionFactory, LoggerBuilder loggerBuilder) {
        this.sessionFactory = sessionFactory;
        this.logger = loggerBuilder.build(DbReplayer.class);
    }

    public String getId(){
        return "db";
    }



    private HibernateSessionFactory sessionFactory;
    private Map<String,DbTreeItem> treeDatabase = new HashMap<>();
    private Map<String,List<DbRow>> straightDatabase = new HashMap<>();
    private JsonTypedSerializer serializer = new JsonTypedSerializer();


    public DbTreeItem getTree(String name){
        return treeDatabase.get(name.toLowerCase(Locale.ROOT));
    }


    private DbTreeItem getLastWithConnectionId(DbTreeItem dbParent, long connectionId) {
        for(var child:dbParent.getChildren()){
            for(var target:child.getTargets()){
                if(target.getConnectionId()==connectionId){
                    return getLastWithConnectionId(child,connectionId);
                }
            }
        }
        return dbParent;
    }
    public void loadDb(Long recordingId) throws Exception {

        if(!hasDbRows(recordingId))return;

        ArrayList<CallIndex> indexes = new ArrayList<>();

        loadIndexes(recordingId, indexes);


        //loadDbTree(recordingId, indexes);
        loadDbStraight(recordingId, indexes);
    }

    private void loadDbStraight(Long recordingId, ArrayList<CallIndex> indexes) throws Exception {
        for(var index : indexes){
            sessionFactory.query(e -> {
                ReplayerRow row = getReplayerRow(recordingId, index, e);
                var reqDeser = serializer.newInstance();
                reqDeser.deserialize(row.getRequest().getRequestText());
                var resDeser = serializer.newInstance();
                resDeser.deserialize(row.getResponse().getResponseText());


                var dbRowName = row.getRequest().getPathParameter("dbName").toLowerCase(Locale.ROOT);
                var dbRow = new DbRow(row,
                        (JdbcCommand) reqDeser.read("command"),
                        (JdbcResult) resDeser.read("result"));
                if(!straightDatabase.containsKey(dbRowName)){
                    straightDatabase.put(dbRowName,new ArrayList<>());
                }
                straightDatabase.get(dbRowName).add(dbRow);
            });
        }
    }

    private void loadDbTree(Long recordingId, ArrayList<CallIndex> indexes) throws Exception {
        Map<String,Map<Long,List<DbRow>>> rows = new HashMap<>();
        loadRowsByConnection(recordingId, indexes, rows);

        for(var db: rows.entrySet()){
            if(!treeDatabase.containsKey(db.getKey())){
                treeDatabase.put(db.getKey(),new DbTreeItem());
            }
            for(var connection:db.getValue().entrySet()){
                for(var dbRow:connection.getValue()){
                    var dbParent = treeDatabase.get(db.getKey());
                    //var targetItemId = db.getKey()+"-"+dbRow.getConnectionId()+"-"+dbRow.getTraceId();
                    if(dbRow.getRequest() instanceof ConnectionConnect){
                        dbParent.addTarget(dbRow);
                        //cache.put(targetItemId,dbRow);
                    }else{
                        var realParent = getLastWithConnectionId(dbParent,dbRow.getConnectionId());

                        var addedAsTarget = false;
                        for (var target : realParent.getTargets()) {
                            //If the request content is the same
                            if (matchesContent(target,dbRow)) {
                                realParent.addTarget(dbRow);
                                addedAsTarget=true;
                                break;
                            }
                        }
                        if(!addedAsTarget){
                            DbTreeItem dbTreeItem = findDbTreeItem(realParent,dbRow);

                            if(dbTreeItem==null) {
                                //Is a nex step into the life of the connection
                                dbTreeItem = new DbTreeItem();
                                dbTreeItem.setParent(realParent);
                                dbTreeItem.addTarget(dbRow);
                                realParent.addChild(dbTreeItem);
                            }else{
                                dbTreeItem.addTarget(dbRow);
                            }
                        }
                    }
                }
            }
        }
    }



    private DbTreeItem findDbTreeItem(DbTreeItem realParent,DbRow dbRow) {
        for(var child:realParent.getChildren()){
            for(var target:child.getTargets()){
                if (matchesContent(target,dbRow)) {
                    return child;
                }
            }
        }
        return null;
    }

    private boolean matchesContent(DbRow target, DbRow dbRow) {
        return target.getRow().getRequest().getRequestText().equalsIgnoreCase(
                dbRow.getRow().getRequest().getRequestText());
    }

    private boolean matchesContent(DbRow target, Request request) {
        return target.getRow().getRequest().getRequestText().equalsIgnoreCase(
                request.getRequestText());
    }

    protected boolean hasRows = false;

    protected boolean hasDbRows(Long recordingId) throws Exception {
        hasRows = (Long)sessionFactory.queryResult(e -> {
            return (Long)e.createQuery("SELECT count(*) FROM ReplayerRow e " +
                            " WHERE " +
                            " e.type='db'" +
                            "AND e.recordingId=" + recordingId)
                    .getResultList().get(0);
        })>0;
        return hasRows;
    }

    private final Logger logger;

    private void loadRowsByConnection(Long recordingId, ArrayList<CallIndex> indexes, Map<String, Map<Long, List<DbRow>>> rows) throws Exception {

        for(var index : indexes){
            sessionFactory.query(e -> {
                ReplayerRow row = getReplayerRow(recordingId, index, e);
                var reqDeser = serializer.newInstance();
                reqDeser.deserialize(row.getRequest().getRequestText());
                var resDeser = serializer.newInstance();
                resDeser.deserialize(row.getResponse().getResponseText());


                var dbRowName = row.getRequest().getPathParameter("dbName").toLowerCase(Locale.ROOT);
                var dbRow = new DbRow(row,
                        (JdbcCommand) reqDeser.read("command"),
                        (JdbcResult) resDeser.read("result"));
                var dbRowConnectionId = dbRow.getConnectionId();
                if(!rows.containsKey(dbRowName)){
                    rows.put(dbRowName,new HashMap<>());
                }

                if(!rows.get(dbRowName).containsKey(dbRowConnectionId)){
                    rows.get(dbRowName).put(dbRowConnectionId,new ArrayList<>());
                }
                rows.get(dbRowName).get(dbRowConnectionId).add(dbRow);
            });
        }
    }

    protected ReplayerRow getReplayerRow(Long recordingId, CallIndex index, EntityManager e) {
        var row =(ReplayerRow) e.createQuery("SELECT e FROM ReplayerRow e " +
                " WHERE " +
                " e.type='db'" +
                " AND e.id ="+ index.getReference()+" " +
                "AND e.recordingId=" + recordingId).getResultList().get(0);
        return row;
    }

    protected void loadIndexes(Long recordingId, ArrayList<CallIndex> indexes) throws Exception {
        sessionFactory.query(e -> {
            addAllIndexes(recordingId, indexes, e);
        });
    }

    protected void addAllIndexes(Long recordingId, ArrayList<CallIndex> indexes, EntityManager e) {
        indexes.addAll(e.createQuery("SELECT e FROM CallIndex e LEFT JOIN ReplayerRow f " +
                " ON e.reference = f.id"+
                " WHERE " +
                " f.type='db' AND e.recordingId=" + recordingId +
                " AND e.stimulatorTest=false ORDER BY e.id ASC").getResultList());
    }

    private Map<Long,Long> connectionShadow = new HashMap<>();
    private Map<Long,List<DbTreeItem>> connectionRealPath = new HashMap<>();
    private AtomicLong atomicLong = new AtomicLong(Long.MAX_VALUE);

    @Override
    public Response findRequestMatch(Request req, String contentHash) throws Exception {
        if(!hasRows)return null;
        var fullPath = req.getPath().substring(1).split("/");
        if(req.getPath().startsWith("/api/db")){
            if(fullPath.length>=5){
                if(req.getPathParameters().size()==0){
                    req.getPathParameters().put("dbName",fullPath[2]);
                    req.getPathParameters().put("targetType",fullPath[3]);
                    req.getPathParameters().put("command",fullPath[4]);
                    if(fullPath.length>=6){
                        req.getPathParameters().put("targetId",fullPath[5]);
                    }
                }
            }
        }
        if(req.getPathParameter("dbName")==null)return null;
        if(!req.getPath().startsWith("/api/db/"))return null;
        var reqDeser = serializer.newInstance();
        reqDeser.deserialize(req.getRequestText());
        var command = (JdbcCommand) reqDeser.read("command");
        var dbName = req.getPathParameter("dbName").toLowerCase(Locale.ROOT);

        //return getTreeMatch(req, command, dbName);
        return getStraightMatch(req, command, dbName);
    }

    private Response getStraightMatch(Request req, JdbcCommand command, String dbName) {
        if(command instanceof Close) {
            var ser = serializer.newInstance();
            var response = new Response();
            ser.write("result", new VoidResult());
            response.getHeaders().put("content-type", "application/json");
            response.setResponseText((String) ser.getSerialized());
            response.setStatusCode(200);
            return response;
        }else if(command instanceof ConnectionConnect){
            var newConnectionId = atomicLong.decrementAndGet();
            var result = new ObjectResult();
            result.setResult(newConnectionId);
            return serialize(result);
        }else{
            var maxValue = 0;
            DbRow target = null;
            var db = straightDatabase.get(dbName);
            for(var row:db){
                if(row.isVisited())continue;
                var current = matchesContentForReplaying(row, command);
                if(current>maxValue){
                    target=row;
                    maxValue=current;
                }
            }
            if(target!=null){
                if(!target.getRow().isStaticRequest()){
                    target.setVisited(true);
                }
                return serialize(target.getResponse());
            }
        }
        return null;
    }

    private Response getTreeMatch(Request req, JdbcCommand command, String dbName) {
        var db = treeDatabase.get(dbName);
        if(command instanceof Close) {
            var ser = serializer.newInstance();
            var response= new Response();
            ser.write("result", new VoidResult());
            response.getHeaders().put("content-type", "application/json");
            response.setResponseText((String) ser.getSerialized());
            response.setStatusCode(200);
            return response;
        }else if(command instanceof ConnectionConnect){
            var newConnectionId = atomicLong.decrementAndGet();
            if(db.getTargets().stream().anyMatch(t->!t.isVisited())){
                var firstNotVisited = db.getTargets().stream().filter(t->!t.isVisited())
                        .findFirst();
                if(firstNotVisited.isEmpty()) return null;
                firstNotVisited.get().setVisited(true);
                connectionRealPath.put(newConnectionId,new ArrayList<>());
                connectionRealPath.get(newConnectionId).add(db);
                var result = new ObjectResult();
                result.setResult(newConnectionId);
                return serialize(result);
            }else{
                return null;
            }
        }else{
            var connectionId = Long.parseLong(req.getHeader("x-connection-id"));
            var path = connectionRealPath.get(connectionId);
            var last = path.get(path.size()-1);
            for(var child:last.getChildren()){
                for(var target:child.getTargets()){
                    if(matchesContentForReplaying(target, command)>0 && !target.isVisited()){
                        var result = target.getResponse();
                        if(child.getChildren().size()==0){
                            if(target.getRow().isStaticRequest()){
                                //Clean the path for static requests
                                for(var i=0;i<path.size();i++){
                                    for(var j=0;j<path.get(i).getTargets().size();j++){
                                        var tth = path.get(i).getTargets().get(j);
                                        if(tth.isVisited()){
                                            tth.setVisited(false);
                                            break;
                                        }
                                    }
                                }
                            }else{
                                target.setVisited(true);
                                connectionRealPath.get(connectionId).clear();
                            }
                        }else{
                            target.setVisited(true);
                            connectionRealPath.get(connectionId).add(child);
                        }
                        //path.add(child);
                        return serialize(result);
                    }
                }
            }

            //No matches
        }
        return null;
    }

    private int matchesContentForReplaying(DbRow target, JdbcCommand command) {
        var possible = target.getRequest();
        var equalityValue=0;
        if(command.getClass()!=possible.getClass())return -1;
        if(ClassUtils.isAssignable(command.getClass(),JdbcSqlCommand.class)){
            var p =(JdbcSqlCommand)possible;
            var c =(JdbcSqlCommand)command;
            if(matchSql(p.getSql(),c.getSql())){
                equalityValue+=10;
            }
        }
        if(ClassUtils.isAssignable(command.getClass(), JdbcSqlBatches.class)){
            var p =(JdbcSqlBatches)possible;
            var c =(JdbcSqlBatches)command;
            if(p.getBatches().size()!=c.getBatches().size()){
                return 0;
            }
            for (int i = 0; i <  p.getBatches().size(); i++) {
                var pp = p.getBatches().get(i);
                var cp = c.getBatches().get(i);
                if(pp.toString().equalsIgnoreCase(cp.toString())){
                    equalityValue+=1;
                }
            }
        }
        if(ClassUtils.isAssignable(command.getClass(), JdbcPreparedStatementParameters.class)){
            var p =(JdbcPreparedStatementParameters)possible;
            var c =(JdbcPreparedStatementParameters)command;
            if(p.getParameters().size()!=c.getParameters().size()){
                return 0;
            }
            for (int i = 0; i <  p.getParameters().size(); i++) {
                var pp = p.getParameters().get(i);
                var cp = c.getParameters().get(i);
                if(pp.toString().equalsIgnoreCase(cp.toString())){
                    equalityValue+=1;
                }
            }
        }
        if(ClassUtils.isAssignable(command.getClass(), JdbcBatchPreparedStatementParameters.class)){
            var p =(JdbcBatchPreparedStatementParameters)possible;
            var c =(JdbcBatchPreparedStatementParameters)command;
            if(p.getBatches().size()!=c.getBatches().size()){
                return 0;
            }
            for (int i = 0; i <  p.getBatches().size(); i++) {
                var pp = p.getBatches().get(i);
                var cp = c.getBatches().get(i);
                if(pp.toString().equalsIgnoreCase(cp.toString())){
                    equalityValue+=1;
                }
            }
        }
        if(possible.toString().equalsIgnoreCase(command.toString())){
            equalityValue=1000;
        }
        return equalityValue;
    }

    private Response serialize(Object result) {
        var ser = serializer.newInstance();
        ser.write("result", result);
        var res = new Response();
        res.addHeader(ConstantsHeader.CONTENT_TYPE, ConstantsMime.JSON);
        res.setResponseText((String) ser.getSerialized());
        return res;
    }

    private boolean matchSql(String possible, String real) {
        if(possible.equalsIgnoreCase(real))return true;
        if(possible.length()!=real.length())return false;
        return false;
    }
}