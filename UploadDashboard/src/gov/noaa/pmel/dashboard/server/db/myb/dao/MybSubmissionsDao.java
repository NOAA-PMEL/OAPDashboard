/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.myb.dao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;

import com.fasterxml.uuid.Generators;

import gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao;
import gov.noaa.pmel.dashboard.server.db.myb.MybatisConnectionFactory;
import gov.noaa.pmel.dashboard.server.db.myb.mappers.SubmissionMapper;
import gov.noaa.pmel.dashboard.server.db.myb.mappers.SubmissionStatusMapper;
import gov.noaa.pmel.dashboard.server.model.SubmissionRecord;
import gov.noaa.pmel.dashboard.server.model.SubmissionStatus;

/**
 * @author kamb
 *
 */
public class MybSubmissionsDao implements SubmissionsDao {

    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao#addDatasetSubmission(java.lang.String, java.lang.String)
     */
    @Override
    public SubmissionRecord addDatasetSubmission(String datasetID, String pkgLocation) throws SQLException {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            SubmissionMapper smapper = (SubmissionMapper) session.getMapper(SubmissionMapper.class);
            Map<String, Object> map = new HashMap<>();
            map.put("_datasetId", datasetID);
            map.put("_archivePkgLocation", pkgLocation);
            smapper.addDatasetSubmission(map); // datasetID, pkgLocation);
            session.commit();
            System.out.println(map);
            SubmissionRecord inserted = smapper.getById((Long)map.get("_dbId"));
            return inserted;
        }
    }

    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao#insert(gov.noaa.pmel.dashboard.server.model.Submission)
     */
    @Override
    public void insert(SubmissionRecord submission) throws SQLException {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            SubmissionMapper smapper = (SubmissionMapper) session.getMapper(SubmissionMapper.class);
            smapper.insertSubmission(submission);
            session.commit();
        }
    }
    @Override
    public SubmissionRecord initialSubmission(SubmissionRecord submission) throws SQLException {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            SubmissionMapper smapper = (SubmissionMapper) session.getMapper(SubmissionMapper.class);
            smapper.initialSubmission(submission);
            SubmissionStatusMapper ssmapper = (SubmissionStatusMapper) session.getMapper(SubmissionStatusMapper.class);
            SubmissionStatus initialStatus = SubmissionStatus.initialStatus(submission.dbId());
            ssmapper.insertStatus(initialStatus);
            session.commit();
            SubmissionRecord inserted = smapper.getById(submission.dbId().longValue());
            return inserted;
        }
    }

//    @Override
//    public void updateSubmission(SubmissionRecord submission) throws SQLException
//    {
//        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
//            SubmissionMapper smapper = (SubmissionMapper) session.getMapper(SubmissionMapper.class);
//            smapper.updateSubmission(submission);
//            session.commit();
//        }
//    }
    
    @Override
    public void updateSubmissionStatus(SubmissionStatus status) throws SQLException
    {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            SubmissionStatusMapper ssmapper = (SubmissionStatusMapper) session.getMapper(SubmissionStatusMapper.class);
            ssmapper.insertStatus(status);
            session.commit();
        }
    }

    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao#getById(long)
     */
    @Override
    public SubmissionRecord getById(long id) throws SQLException {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            SubmissionMapper smapper = (SubmissionMapper) session.getMapper(SubmissionMapper.class);
            SubmissionRecord sr = smapper.getById(id);
            return sr;
        }
    }

//    @Override
//    public SubmissionRecord getFullById(long id) throws SQLException {
//        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
//            SubmissionMapper smapper = (SubmissionMapper) session.getMapper(SubmissionMapper.class);
//            SubmissionRecord sr = smapper.getFullById(id);
//            return sr;
//        }
//    }

    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao#getByVersionKey(java.lang.String, int)
     */
    @Override
    public SubmissionRecord getByVersionKey(String key, int version) throws SQLException {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            SubmissionMapper smapper = (SubmissionMapper) session.getMapper(SubmissionMapper.class);
            SubmissionRecord sr = smapper.getVersionByKey(key, version);
            return sr;
        }
    }

    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao#getLatestByKey(java.lang.String)
     */
    @Override
    public SubmissionRecord getLatestByKey(String key) throws SQLException {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            SubmissionMapper smapper = (SubmissionMapper) session.getMapper(SubmissionMapper.class);
            SubmissionRecord sr = smapper.getLatestByKey(key);
            return sr;
        }
    }

    @Override
    public SubmissionRecord getLatestForDataset(String datasetId) throws SQLException {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            SubmissionMapper smapper = (SubmissionMapper) session.getMapper(SubmissionMapper.class);
            SubmissionRecord sr = smapper.getLatestForDatasetId(datasetId);
            return sr;
        }
    }

    @Override
    public SubmissionRecord getVersionForDataset(String datasetId, int version) throws SQLException {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            SubmissionMapper smapper = (SubmissionMapper) session.getMapper(SubmissionMapper.class);
            SubmissionRecord sr = smapper.getVersionForDatasetId(datasetId, version);
            return sr;
        }
    }

    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao#getAllVersionsByKey(java.lang.String)
     */
    @Override
    public List<SubmissionRecord> getAllVersionsByKey(String key) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public static void main(String[] args) {
        try {
//            UUID u1 = Generators.timeBasedGenerator().generate();
//            System.out.println("u1:" + u1.toString());
//            Thread.sleep(5000);
//            UUID u2 = Generators.timeBasedGenerator().generate();
//            System.out.println("u2:" + u2.toString());
//            UUID u3 = UUID.randomUUID();
//            System.out.println("u3:" + u3.toString());
            
            SubmissionRecord s = SubmissionRecord.builder()
                    .datasetId("PRISM92")
//                    .submissionKey(Generators.timeBasedGenerator().generate().toString())
                    .archiveBag("bags/PRISM/PRISM92/")
                    .build();
           MybSubmissionsDao msd = new MybSubmissionsDao();
           SubmissionRecord sr = msd.getById(32);
           System.out.println(sr);
//           sr = msd.getFullById(32);
//           System.out.println(sr);
//           long dbid = msd.insertFirstSubmission(s);
//           System.out.println("dbId:"+dbid);
//           System.out.println(s);
//           s = msd.addDatasetSubmission("firstDataset", "Somehwere over the rainbow");
//           System.out.println(s);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }
    }
}
