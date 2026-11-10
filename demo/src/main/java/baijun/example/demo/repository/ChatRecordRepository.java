package baijun.example.demo.repository;

import baijun.example.demo.entity.ChatRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRecordRepository extends JpaRepository<ChatRecord, Long> {

    List<ChatRecord> findTop100ByReceiverIsNullOrderByIdDesc();

    @Query("select r from ChatRecord r where r.receiver is not null "
            + "and ((r.sender = :a and r.receiver = :b) or (r.sender = :b and r.receiver = :a)) "
            + "order by r.id desc")
    List<ChatRecord> findPrivateBetween(@Param("a") String a, @Param("b") String b, Pageable pageable);
}
