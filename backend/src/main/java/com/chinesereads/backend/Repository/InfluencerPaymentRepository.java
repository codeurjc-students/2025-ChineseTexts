package com.chinesereads.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chinesereads.backend.Model.InfluencerPayment;

public interface InfluencerPaymentRepository extends JpaRepository<InfluencerPayment, Long> {

    boolean existsByInvoiceId(String invoiceId);
}
