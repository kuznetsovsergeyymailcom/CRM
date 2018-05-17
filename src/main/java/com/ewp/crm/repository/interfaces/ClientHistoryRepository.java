package com.ewp.crm.repository.interfaces;

import com.ewp.crm.models.Client;
import com.ewp.crm.models.ClientHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientHistoryRepository extends JpaRepository<ClientHistory, Long> {
}