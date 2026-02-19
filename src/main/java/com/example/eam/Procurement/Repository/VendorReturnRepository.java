package com.example.eam.Procurement.Repository;

import com.example.eam.Procurement.Entity.VendorReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VendorReturnRepository extends JpaRepository<VendorReturn, Long> {

    @Query("""
        select r from VendorReturn r
        where (:grnId is null or r.grnId = :grnId)
          and (:vendorId is null or r.vendorId = :vendorId)
          and (:itemId is null or r.itemId = :itemId)
        order by r.id desc
    """)
    List<VendorReturn> findByFilters(@Param("grnId") Long grnId,
                                     @Param("vendorId") Long vendorId,
                                     @Param("itemId") Long itemId);
}
