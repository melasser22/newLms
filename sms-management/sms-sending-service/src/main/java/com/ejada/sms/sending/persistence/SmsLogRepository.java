package com.ejada.sms.sending.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsLogRepository extends JpaRepository<SmsLog, String> {
}
