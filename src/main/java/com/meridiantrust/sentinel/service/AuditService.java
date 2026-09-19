/*
 * Sentinel AML — Real-Time Money Laundering Detection Platform
 * Copyright (C) 2026 Chandan Kumar Shaw <shawchandankumar20@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.meridiantrust.sentinel.service;

import com.meridiantrust.sentinel.domain.AuditEvent;
import com.meridiantrust.sentinel.repository.AuditEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the immutable audit trail for alert/case state transitions
 * (Auditability NFR). Records are append-only; there is no update or delete path.
 */
@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String entityType, String entityId, String action, String actor,
                       String previousState, String newState, String details) {
        AuditEvent event = new AuditEvent();
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setAction(action);
        event.setActor(actor);
        event.setPreviousState(previousState);
        event.setNewState(newState);
        event.setDetails(details);
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> trail(String entityType, String entityId) {
        return repository.findByEntityTypeAndEntityIdOrderByOccurredAtAsc(entityType, entityId);
    }
}
