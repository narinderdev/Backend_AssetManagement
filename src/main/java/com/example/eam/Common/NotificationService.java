package com.example.eam.Common;

import com.example.eam.Technician.Entity.Technician;
import com.example.eam.Technician.Entity.TechnicianDeviceToken;
import com.example.eam.Technician.Repository.TechnicianDeviceTokenRepository;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember;
import com.example.eam.TechnicianTeam.Repository.TechnicianTeamMemberRepository;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final FirebaseMessaging firebaseMessaging;
    private final TechnicianDeviceTokenRepository technicianDeviceTokenRepository;
    private final TechnicianTeamMemberRepository technicianTeamMemberRepository;

    public void sendWorkOrderAssigned(WorkOrder wo, Technician technician, Long teamId) {
        if (wo == null) return;
        String title = "New Work Order Assigned";
        String body = wo.getWoTitle() != null
                ? wo.getWoTitle()
                : "Work Order " + wo.getWorkOrderId();

        if (technician != null) {
            sendToTechnician(technician, title, body, wo);
        }
        if (teamId != null) {
            // notify team leader(s)
            List<TechnicianTeamMember> members = technicianTeamMemberRepository.findByTeam_Id(teamId);
            members.stream()
                    .filter(TechnicianTeamMember::isTeamLeader)
                    .map(TechnicianTeamMember::getTechnician)
                    .forEach(leader -> sendToTechnician(leader, title, body, wo));
        }
    }

    private void sendToTechnician(Technician technician, String title, String body, WorkOrder wo) {
        if (technician == null || technician.getId() == null) return;
        List<TechnicianDeviceToken> tokens = technicianDeviceTokenRepository.findByTechnician_Id(technician.getId());
        for (TechnicianDeviceToken token : tokens) {
            if (token.getDeviceToken() == null || token.getDeviceToken().isBlank()) continue;
            try {
                Message message = Message.builder()
                        .setToken(token.getDeviceToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("workOrderId", wo != null && wo.getId() != null ? String.valueOf(wo.getId()) : "")
                        .putData("workOrderCode", wo != null ? String.valueOf(wo.getWorkOrderId()) : "")
                        .putData("workOrderTitle", wo != null && wo.getWoTitle() != null ? wo.getWoTitle() : "")
                        .build();
                firebaseMessaging.send(message);
            } catch (Exception e) {
                log.warn("Failed to send FCM to technician {} token {}", technician.getId(), token.getId(), e);
                handleSendFailure(e, token);
            }
        }
    }

private void handleSendFailure(Exception e, TechnicianDeviceToken token) {
    if (e instanceof FirebaseMessagingException fme) {
        MessagingErrorCode code = fme.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED) {
            try {
                technicianDeviceTokenRepository.delete(token);
                log.info("Deleted unregistered device token {}", token.getId());
            } catch (Exception ex) {
                log.warn("Failed to delete unregistered token {}", token.getId(), ex);
            }
        }
    }
}
}
