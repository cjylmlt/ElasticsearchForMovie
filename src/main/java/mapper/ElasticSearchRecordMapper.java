package mapper;

import data.ElasticSearchRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ElasticSearchRecordMapper {
    public List<ElasticSearchRecord> getElasticSearchRecord(@Param("start") int start,@Param("num") int num);
}
