package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    private Notification testNotification;
    private UserAccount testUser;

    @BeforeEach
    public void setUp() {
        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testNotification = Notification.builder()
                .id(1L)
                .recipient(testUser)
                .title("Test Notification")
                .message("Test Message")
                .type(NotificationType.EVENT)
                .status(NotificationStatus.UNREAD)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    public void testCreateNotification_ShouldReturnSavedNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.createNotification(
                testUser,
                "Test Notification",
                "Test Message",
                NotificationType.EVENT
        );

        assertNotNull(result);
        assertEquals("Test Notification", result.getTitle());
        assertEquals(NotificationType.EVENT, result.getType());
        assertEquals(NotificationStatus.UNREAD, result.getStatus());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    public void testCreateNotificationWithAllParams_ShouldReturnSavedNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.createNotification(
                testUser,
                "Test Notification",
                "Test Message",
                NotificationType.EVENT,
                "/events/1",
                1L,
                "Event",
                Prioridade.ALTA
        );

        assertNotNull(result);
        assertEquals("Test Notification", result.getTitle());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    public void testFindById_WhenNotificationExists_ShouldReturnNotification() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        Optional<Notification> result = notificationService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Test Notification", result.get().getTitle());
        verify(notificationRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_WhenNotificationDoesNotExist_ShouldReturnEmpty() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Notification> result = notificationService.findById(1L);

        assertFalse(result.isPresent());
        verify(notificationRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindByRecipient_ShouldReturnNotificationList() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(testUser))
                .thenReturn(notifications);

        List<Notification> result = notificationService.findByRecipient(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Notification", result.get(0).getTitle());
        verify(notificationRepository, times(1)).findByRecipientOrderByCreatedAtDesc(testUser);
    }

    @Test
    public void testFindByRecipientWithPagination_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(Arrays.asList(testNotification));
        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(testUser, pageable))
                .thenReturn(page);

        Page<Notification> result = notificationService.findByRecipient(testUser, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(notificationRepository, times(1)).findByRecipientOrderByCreatedAtDesc(testUser, pageable);
    }

    @Test
    public void testFindUnreadByRecipient_ShouldReturnUnreadNotifications() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByRecipientAndStatusOrderByCreatedAtDesc(
                testUser, NotificationStatus.UNREAD))
                .thenReturn(notifications);

        List<Notification> result = notificationService.findUnreadByRecipient(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(NotificationStatus.UNREAD, result.get(0).getStatus());
        verify(notificationRepository, times(1))
                .findByRecipientAndStatusOrderByCreatedAtDesc(testUser, NotificationStatus.UNREAD);
    }

    @Test
    public void testFindArchivedByRecipient_ShouldReturnArchivedNotifications() {
        testNotification.setStatus(NotificationStatus.ARCHIVED);
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByRecipientAndStatusOrderByCreatedAtDesc(
                testUser, NotificationStatus.ARCHIVED))
                .thenReturn(notifications);

        List<Notification> result = notificationService.findArchivedByRecipient(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(NotificationStatus.ARCHIVED, result.get(0).getStatus());
        verify(notificationRepository, times(1))
                .findByRecipientAndStatusOrderByCreatedAtDesc(testUser, NotificationStatus.ARCHIVED);
    }

    @Test
    public void testFindArchivedByRecipientWithPagination_ShouldReturnPage() {
        testNotification.setStatus(NotificationStatus.ARCHIVED);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(Arrays.asList(testNotification));
        when(notificationRepository.findByRecipientAndStatusOrderByCreatedAtDesc(
                testUser, NotificationStatus.ARCHIVED, pageable))
                .thenReturn(page);

        Page<Notification> result = notificationService.findArchivedByRecipient(testUser, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(NotificationStatus.ARCHIVED, result.getContent().get(0).getStatus());
        verify(notificationRepository, times(1))
                .findByRecipientAndStatusOrderByCreatedAtDesc(testUser, NotificationStatus.ARCHIVED, pageable);
    }

    @Test
    public void testCountUnread_ShouldReturnCount() {
        when(notificationRepository.countUnreadByRecipient(testUser)).thenReturn(5L);

        long count = notificationService.countUnread(testUser);

        assertEquals(5L, count);
        verify(notificationRepository, times(1)).countUnreadByRecipient(testUser);
    }

    @Test
    public void testMarkAsRead_ShouldUpdateNotificationStatus() {
        testNotification.setStatus(NotificationStatus.UNREAD);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.markAsRead(1L);

        assertNotNull(result);
        assertEquals(NotificationStatus.READ, result.getStatus());
        assertNotNull(result.getReadAt());
        verify(notificationRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    public void testMarkAsUnread_ShouldUpdateNotificationStatus() {
        testNotification.setStatus(NotificationStatus.READ);
        testNotification.setReadAt(LocalDateTime.now());
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.markAsUnread(1L);

        assertNotNull(result);
        assertEquals(NotificationStatus.UNREAD, result.getStatus());
        assertNull(result.getReadAt());
        verify(notificationRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    public void testMarkAllAsRead_ShouldReturnUpdatedCount() {
        when(notificationRepository.markAllAsReadForRecipient(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(10);

        int count = notificationService.markAllAsRead(testUser);

        assertEquals(10, count);
        verify(notificationRepository, times(1))
                .markAllAsReadForRecipient(eq(testUser), any(LocalDateTime.class));
    }

    @Test
    public void testArchiveNotification_ShouldUpdateStatus() {
        testNotification.setStatus(NotificationStatus.READ);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.archiveNotification(1L);

        assertNotNull(result);
        assertEquals(NotificationStatus.ARCHIVED, result.getStatus());
        verify(notificationRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    public void testDeleteNotification_WhenExists_ShouldDeleteSuccessfully() {
        when(notificationRepository.existsById(1L)).thenReturn(true);
        doNothing().when(notificationRepository).deleteById(1L);

        assertDoesNotThrow(() -> notificationService.deleteNotification(1L));

        verify(notificationRepository, times(1)).existsById(1L);
        verify(notificationRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testDeleteNotification_WhenDoesNotExist_ShouldThrowException() {
        when(notificationRepository.existsById(1L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.deleteNotification(1L));

        assertEquals("Notificação não encontrada com id 1", exception.getMessage());
        verify(notificationRepository, times(1)).existsById(1L);
        verify(notificationRepository, never()).deleteById(1L);
    }

    @Test
    public void testNotifyNewEvent_ShouldCreateEventNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.notifyNewEvent(testUser, 1L, "Novo evento criado");

        assertNotNull(result);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    public void testNotifyEventUpdate_ShouldCreateEventNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.notifyEventUpdate(testUser, 1L, "Evento atualizado");

        assertNotNull(result);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    public void testNotifyNewDemand_ShouldCreateDemandNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.notifyNewDemand(testUser, 1L, "Nova demanda");

        assertNotNull(result);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    public void testNotifyAlert_ShouldCreateAlertNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.notifyAlert(testUser, "Alerta", "Mensagem de alerta");

        assertNotNull(result);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    public void testFindHighPriorityUnread_ShouldReturnHighPriorityNotifications() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findHighPriorityUnreadByRecipient(testUser))
                .thenReturn(notifications);

        List<Notification> result = notificationService.findHighPriorityUnread(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository, times(1)).findHighPriorityUnreadByRecipient(testUser);
    }

    @Test
    public void testFindRecentNotifications_ShouldReturnRecentNotifications() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findRecentNotifications(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(notifications);

        List<Notification> result = notificationService.findRecentNotifications(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository, times(1))
                .findRecentNotifications(eq(testUser), any(LocalDateTime.class));
    }

    @Test
    public void testFindByRelatedEntity_ShouldReturnRelatedNotifications() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByRelatedEntityTypeAndRelatedEntityId("Event", 1L))
                .thenReturn(notifications);

        List<Notification> result = notificationService.findByRelatedEntity("Event", 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository, times(1))
                .findByRelatedEntityTypeAndRelatedEntityId("Event", 1L);
    }
}
