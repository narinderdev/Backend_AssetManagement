package com.example.eam.Procurement.Repository;

import com.example.eam.Procurement.Entity.GoodsReceiptNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, Long> {

    boolean existsByPoId(Long poId);
    boolean existsByPoIdAndCompanyId(Long poId, Long companyId);

    List<GoodsReceiptNote> findByPoId(Long poId);
    List<GoodsReceiptNote> findByPoIdAndCompanyId(Long poId, Long companyId);

    List<GoodsReceiptNote> findByDayKeyUtcBetween(String fromDayKeyInclusive, String toDayKeyInclusive);
    List<GoodsReceiptNote> findByDayKeyUtcBetweenAndCompanyId(String fromDayKeyInclusive, String toDayKeyInclusive, Long companyId);

    List<GoodsReceiptNote> findByDayKeyUtcStartingWith(String prefix);
    List<GoodsReceiptNote> findByDayKeyUtcStartingWithAndCompanyId(String prefix, Long companyId);

    java.util.Optional<GoodsReceiptNote> findByIdAndCompanyId(Long id, Long companyId);
    List<GoodsReceiptNote> findByCompanyId(Long companyId);
}
