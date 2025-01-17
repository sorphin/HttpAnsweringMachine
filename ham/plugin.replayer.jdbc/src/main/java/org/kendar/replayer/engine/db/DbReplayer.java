package org.kendar.replayer.engine.db;

import org.apache.commons.lang3.ClassUtils;
import org.kendar.janus.TraceAwareType;
import org.kendar.janus.cmd.Close;
import org.kendar.janus.cmd.connection.ConnectionConnect;
import org.kendar.janus.cmd.interfaces.*;
import org.kendar.janus.results.JdbcResult;
import org.kendar.janus.results.ObjectResult;
import org.kendar.janus.results.VoidResult;
import org.kendar.janus.serialization.JsonTypedSerializer;
import org.kendar.replayer.engine.ReplayerEngine;
import org.kendar.replayer.engine.RequestMatch;
import org.kendar.replayer.engine.db.sqlsim.SqlSimulator;
import org.kendar.replayer.storage.CallIndex;
import org.kendar.replayer.storage.DbRecording;
import org.kendar.replayer.storage.ReplayerRow;
import org.kendar.servers.JsonConfiguration;
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
import java.util.stream.Collectors;

@Component
public class DbReplayer implements ReplayerEngine {


    private final SqlSimulator simulator = new SqlSimulator();
    private final HibernateSessionFactory sessionFactory;
    private final Map<String, List<DbRow>> straightDatabase = new HashMap<>();
    private final JsonTypedSerializer serializer = new JsonTypedSerializer();
    private final Logger logger;
    private final Map<Long, Long> connectionShadow = new HashMap<>();
    private final Map<Long, List<DbTreeItem>> connectionRealPath = new HashMap<>();
    private final AtomicLong atomicLong = new AtomicLong(Long.MAX_VALUE);
    protected boolean hasRows = false;
    private boolean useSimEngine;
    public DbReplayer(HibernateSessionFactory sessionFactory, LoggerBuilder loggerBuilder, JsonConfiguration configuration) {
        this.sessionFactory = sessionFactory;
        this.logger = loggerBuilder.build(DbReplayer.class);
    }

    @Override
    public ReplayerEngine create(LoggerBuilder loggerBuilder) {
        return new DbReplayer(sessionFactory, loggerBuilder, null);
    }

    @Override
    public boolean isValidPath(Request req) {
        return req.getPath().startsWith("/api/db/");
    }

    @Override
    public boolean isValidRoundTrip(Request req, Response res, Map<String, String> specialParams) {
        var recordDbCalls = specialParams.get("recordDbCalls") == null ? false :
                Boolean.parseBoolean(specialParams.get("recordDbCalls"));
        var recordVoidDbCalls = specialParams.get("recordVoidDbCalls") == null ? false :
                Boolean.parseBoolean(specialParams.get("recordVoidDbCalls"));
        var doUseSimEngine = specialParams.get("useSimEngine") == null ? false :
                Boolean.parseBoolean(specialParams.get("useSimEngine"));
        var dbNames = specialParams.get("dbNames") == null ? new String[]{"*"} :
                specialParams.get("dbNames").trim().split(",");
        var result = true;
        if (!recordDbCalls) result = false;
        if (!recordVoidDbCalls) {
            if (res.getResponseText() == null || res.getResponseText().contains("VoidResult")) {
                result = false;
            }
        }
        boolean dbNameAllowed = isDbNameAllowed(req, dbNames);
        if (!dbNameAllowed) {
            result = false;
        }
        if (doUseSimEngine && result) {
            var connectionId = req.getHeader("X-Connection-Id") == null ? -1L :
                    Long.parseLong(req.getHeader("X-Connection-Id"));
            var deser = serializer.newInstance();
            deser.deserialize(req.getRequestText());
            var simResponse = simulator.handle(deser.read("command"), connectionId);
            if (simResponse == null || !simResponse.isHasResponse()) {
                result = true;
            } else {
                result = false;
            }
        }
        return result;
    }

    private boolean isDbNameAllowed(Request req, String[] dbNames) {
        var dbNameAllowed = false;
        for (var dbName : dbNames) {
            if (dbName.equalsIgnoreCase("*")) {
                dbNameAllowed = true;
                break;
            } else if (dbName.equalsIgnoreCase(req.getPathParameter("dbName"))) {
                dbNameAllowed = true;
                break;
            }
        }
        return dbNameAllowed;
    }

    @Override
    public void setParams(Map<String, String> specialParams) {
        useSimEngine = specialParams.get("useSimEngine") == null ? false :
                Boolean.parseBoolean(specialParams.get("useSimEngine"));
    }

    public String getId() {
        return "db";
    }

    public void loadDb(Long recordingId) throws Exception {

        if (!hasDbRows(recordingId)) return;

        ArrayList<CallIndex> indexes = new ArrayList<>();

        loadIndexes(recordingId, indexes);


        //loadDbTree(recordingId, indexes);
        loadDbStraight(recordingId, indexes);
    }

    private void loadDbStraight(Long recordingId, ArrayList<CallIndex> indexes) throws Exception {
        for (var index : indexes) {
            sessionFactory.query(e -> {
                ReplayerRow row = getReplayerRow(recordingId, index, e);
                if (row == null) return;
                var reqDeser = serializer.newInstance();
                reqDeser.deserialize(row.getRequest().getRequestText());
                var resDeser = serializer.newInstance();
                resDeser.deserialize(row.getResponse().getResponseText());


                var dbRowName = row.getRequest().getPathParameter("dbName").toLowerCase(Locale.ROOT);
                var dbRow = new DbRow(row,
                        (JdbcCommand) reqDeser.read("command"),
                        (JdbcResult) resDeser.read("result"));
                if (!straightDatabase.containsKey(dbRowName)) {
                    straightDatabase.put(dbRowName, new ArrayList<>());
                }
                straightDatabase.get(dbRowName).add(dbRow);
            });
        }
    }

    protected boolean hasDbRows(Long recordingId) throws Exception {
        hasRows = (Long) sessionFactory.queryResult(e -> (Long) e.createQuery("SELECT count(*) FROM ReplayerRow e " +
                        " WHERE " +
                        " e.type='db'" +
                        "AND e.recordingId=" + recordingId)
                .getResultList().get(0)) > 0;
        return hasRows;
    }

    protected ReplayerRow getReplayerRow(Long recordingId, CallIndex index, EntityManager e) {
        List<ReplayerRow> rs = e.createQuery("SELECT e FROM ReplayerRow e " +
                " WHERE " +
                " e.type='db'" +
                " AND e.id =" + index.getReference() + " " +
                "AND e.recordingId=" + recordingId).getResultList();
        if (rs.size() > 0) {
            return (ReplayerRow) rs.get(0);
        }
        return null;
    }

    protected void loadIndexes(Long recordingId, ArrayList<CallIndex> indexes) throws Exception {
        sessionFactory.query(e -> addAllIndexes(recordingId, indexes, e));
    }

    protected void addAllIndexes(Long recordingId, ArrayList<CallIndex> indexes, EntityManager e) {
        var rs = e.createQuery("SELECT e FROM CallIndex e LEFT JOIN ReplayerRow f " +
                " ON e.reference = f.id" +
                " WHERE " +
                " f.type='" + this.getId() + "' AND e.recordingId=" + recordingId +
                " AND e.stimulatorTest=false ORDER BY e.id ASC").getResultList();
        var founded = new HashSet<Long>();
        for (var rss : rs) {
            e.detach(rss);
            var index = (CallIndex) rss;
            if (founded.contains(index.getIndex())) continue;
            indexes.add(index);
            founded.add(index.getIndex());
        }
    }

    @Override
    public RequestMatch findRequestMatch(Request req, String contentHash, Map<String, String> specialParams) throws Exception {
        if (!hasRows) return null;
        var fullPath = req.getPath().substring(1).split("/");
        if (req.getPath().startsWith("/api/db")) {
            if (fullPath.length >= 5) {
                if (req.getPathParameters().size() == 0) {
                    req.getPathParameters().put("dbName", fullPath[2]);
                    req.getPathParameters().put("targetType", fullPath[3]);
                    req.getPathParameters().put("command", fullPath[4]);
                    if (fullPath.length >= 6) {
                        req.getPathParameters().put("targetId", fullPath[5]);
                    }
                }
            }
        } else {
            return null;
        }
        if (req.getPathParameter("dbName") == null) return null;
        if (!req.getPath().startsWith("/api/db/")) return null;

        var dbNames = specialParams.get("dbNames") == null ? new String[]{"*"} :
                specialParams.get("dbNames").trim().split(",");

        boolean dbNameAllowed = isDbNameAllowed(req, dbNames);
        if (!dbNameAllowed) {
            return null;
        }
        var reqDeser = serializer.newInstance();
        reqDeser.deserialize(req.getRequestText());
        var command = (JdbcCommand) reqDeser.read("command");
        var dbName = req.getPathParameter("dbName").toLowerCase(Locale.ROOT);

        //return getTreeMatch(req, command, dbName);
        var result = getStraightMatch(req, command, dbName);
        return new RequestMatch(req, null, result);
    }

    private Response getStraightMatch(Request req, JdbcCommand command, String dbName) {
        var connectionId = req.getHeader("X-Connection-Id") == null ? -1L :
                Long.parseLong(req.getHeader("X-Connection-Id"));
        if (command instanceof Close) {
            var ser = serializer.newInstance();
            var response = new Response();
            ser.write("result", new VoidResult());
            response.getHeaders().put("content-type", "application/json");
            response.setResponseText((String) ser.getSerialized());
            response.setStatusCode(200);
            return response;
        } else if (command instanceof ConnectionConnect) {
            var newConnectionId = atomicLong.decrementAndGet();
            var result = new ObjectResult();
            result.setResult(newConnectionId);
            return serialize(result);
        } else {
            var maxValue = 0;
            DbRow target = null;
            var db = straightDatabase.get(dbName);
            for (var row : db) {
                if (row.isVisited()) continue;
                var current = matchesContentForReplaying(row, command);
                if (current > maxValue) {
                    target = row;
                    maxValue = current;
                }
            }
            if (target != null) {
                if (!target.getRow().isStaticRequest()) {
                    target.setVisited(true);
                }
                return serialize(target.getResponse());
            }

        }
        if (useSimEngine) {
            var simResponse = simulator.handle(command, connectionId);
            if (simResponse != null && simResponse.isHasResponse()) {
                return serialize(simResponse.getResponse());
            }
        }

        logger.error("NO MATCH FOR " + command.toString());
        return serialize(new VoidResult());
    }

    private int matchesContentForReplaying(DbRow target, JdbcCommand command) {
        var possible = target.getRequest();
        var equalityValue = 0;
        if (command.getClass() != possible.getClass()) return -1;
        if (ClassUtils.isAssignable(command.getClass(), JdbcSqlCommand.class)) {
            var p = (JdbcSqlCommand) possible;
            var c = (JdbcSqlCommand) command;
            if (matchSql(p.getSql(), c.getSql())) {
                equalityValue += 10;
            }
        }
        if (ClassUtils.isAssignable(command.getClass(), JdbcSqlBatches.class)) {
            var p = (JdbcSqlBatches) possible;
            var c = (JdbcSqlBatches) command;
            if (p.getBatches().size() != c.getBatches().size()) {
                return 0;
            }
            for (int i = 0; i < p.getBatches().size(); i++) {
                var pp = p.getBatches().get(i);
                var cp = c.getBatches().get(i);
                if (pp.toString().equalsIgnoreCase(cp.toString())) {
                    equalityValue += 1;
                }
            }
        }
        if (ClassUtils.isAssignable(command.getClass(), JdbcPreparedStatementParameters.class)) {
            var p = (JdbcPreparedStatementParameters) possible;
            var c = (JdbcPreparedStatementParameters) command;
            if (p.getParameters().size() != c.getParameters().size()) {
                return 0;
            }
            for (int i = 0; i < p.getParameters().size(); i++) {
                var pp = p.getParameters().get(i);
                var cp = c.getParameters().get(i);
                if (pp.toString().equalsIgnoreCase(cp.toString())) {
                    equalityValue += 1;
                }
            }
        }
        if (ClassUtils.isAssignable(command.getClass(), JdbcBatchPreparedStatementParameters.class)) {
            var p = (JdbcBatchPreparedStatementParameters) possible;
            var c = (JdbcBatchPreparedStatementParameters) command;
            if (p.getBatches().size() != c.getBatches().size()) {
                return 0;
            }
            for (int i = 0; i < p.getBatches().size(); i++) {
                var pp = p.getBatches().get(i);
                var cp = c.getBatches().get(i);
                if (pp.toString().equalsIgnoreCase(cp.toString())) {
                    equalityValue += 1;
                }
            }
        }
        if (possible.toString().equalsIgnoreCase(command.toString())) {
            equalityValue = 1000;
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
        if (possible.equalsIgnoreCase(real)) return true;
        return false;
    }

    @Override
    public void setupStaticCalls(DbRecording recording) throws Exception {
        ArrayList<CallIndex> indexes = new ArrayList<>();
        HashMap<String, List<Long>> mappingIndexes = new HashMap<>();
        HashMap<String, Set<String>> mappingResponses = new HashMap<>();
        loadIndexes(recording.getId(), indexes);
        for (var index : indexes) {
            sessionFactory.query(e -> {
                Object[] crcPath = (Object[]) e.createQuery("SELECT " +
                        " c.requestHash,c.path,c.responseHash " +
                        " FROM ReplayerRow c WHERE c.recordingId=" + recording.getId() + " AND " +
                        " c.id=" + index.getReference()).getResultList().get(0);

                var requestHash = (String) crcPath[0];
                var path = (String) crcPath[1];
                var responseHash = (String) crcPath[2];
                if (path.matches(".*/[0-9]+")) {
                    var arr = path.split("/");
                    String[] subarray = new String[arr.length - 1];
                    System.arraycopy(arr, 0, subarray, 0, subarray.length);
                    path = String.join("/", subarray);
                }
                //var crc = row.getRequestHash()+":"+row.getResponseHash();
                if (!mappingIndexes.containsKey(requestHash + path)) {
                    mappingIndexes.put(requestHash + path, new ArrayList<>());
                    mappingResponses.put(requestHash + path, new HashSet<>());
                }
                mappingIndexes.get(requestHash + path).add(index.getId());
                mappingResponses.get(requestHash + path).add(responseHash);
            });
        }
        for (var mappingIndex : mappingIndexes.entrySet()) {
            if (mappingIndex.getValue().size() > 1 && mappingResponses.get(mappingIndex.getKey()).size() == 1) {

                sessionFactory.transactional(e -> {
                    var first = mappingIndex.getValue().get(0);

                    var callIndexesToRemove = mappingIndex.getValue().stream().skip(1)
                            .map(Object::toString)
                            .collect(Collectors.toList());
                    var callIndex = (CallIndex) e.createQuery("SELECT e FROM CallIndex e " +
                            " WHERE " +
                            " e.recordingId=" + recording.getId() +
                            " AND e.id=" + first).getResultList().get(0);
                    callIndex.setCalls(callIndexesToRemove.size() + 1);
                    var row = (ReplayerRow) e.createQuery("SELECT e FROM ReplayerRow e " +
                            " WHERE " +
                            " e.recordingId=" + recording.getId() +
                            " AND e.id=" + callIndex.getReference()).getResultList().get(0);
                    row.setStaticRequest(true);
                    e.merge(callIndex);
                    e.merge(row);


                    var referencesToRemove = ((List<Long>) e.createQuery("SELECT e.reference FROM CallIndex e " +
                            " WHERE " +
                            " e.recordingId=" + recording.getId() +
                            " AND e.id IN (" + String.join(",", callIndexesToRemove) + ")").getResultList())
                            .stream().map(Object::toString).collect(Collectors.toList());
                    e.createQuery("DELETE FROM  ReplayerRow e " +
                            " WHERE " +
                            " e.recordingId=" + recording.getId() +
                            " AND e.id IN (" + String.join(",", referencesToRemove) + ")").executeUpdate();
                    e.createQuery("DELETE FROM  CallIndex e " +
                            " WHERE " +
                            " e.recordingId=" + recording.getId() +
                            " AND e.id IN (" + String.join(",", callIndexesToRemove) + ")").executeUpdate();


                });
            }
        }

    }

    @Override
    public void updateReqRes(Request req, Response res, Map<String, String> specialParams) {

        var reqDeser = serializer.newInstance();
        reqDeser.deserialize(req.getRequestText());
        var cmd = reqDeser.read("command");
        if (ClassUtils.isAssignable(cmd.getClass(), TraceAwareType.class)) {
            var oriTraceId = ((TraceAwareType) cmd).getTraceId();
            req.getHeaders().put("X-ORI-TRACE-ID", String.valueOf(oriTraceId));
            ((TraceAwareType) cmd).setTraceId(0);
            var reqSer = serializer.newInstance();
            reqSer.write("command", cmd);
            req.setRequestText((String) reqSer.getSerialized());
        }
        var resDeser = serializer.newInstance();
        resDeser.deserialize(res.getResponseText());
        var result = resDeser.read("result");
        if (ClassUtils.isAssignable(result.getClass(), TraceAwareType.class)) {
            var oriTraceId = ((TraceAwareType) result).getTraceId();
            res.getHeaders().put("X-ORI-TRACE-ID", String.valueOf(oriTraceId));
            ((TraceAwareType) result).setTraceId(0);
            var resSer = serializer.newInstance();
            resSer.write("result", result);
            res.setResponseText((String) resSer.getSerialized());
        }
    }
}
