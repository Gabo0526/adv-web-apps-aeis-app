package ec.edu.epn.fis.aeis.help.service;

import ec.edu.epn.fis.aeis.help.dto.CreateTicketRequestDTO;
import ec.edu.epn.fis.aeis.help.dto.MessageDTO;
import ec.edu.epn.fis.aeis.help.dto.TicketDTO;
import ec.edu.epn.fis.aeis.help.exception.ForbiddenException;
import ec.edu.epn.fis.aeis.help.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.help.model.document.Message;
import ec.edu.epn.fis.aeis.help.model.document.Ticket;
import ec.edu.epn.fis.aeis.help.model.enums.SenderRole;
import ec.edu.epn.fis.aeis.help.model.enums.TicketStatus;
import ec.edu.epn.fis.aeis.help.repository.MessageRepository;
import ec.edu.epn.fis.aeis.help.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, messageRepository, messagingTemplate);
    }

    private Ticket ticket(String id, String userId, TicketStatus status, Instant createdAt) {
        return Ticket.builder()
                .id(id).userId(userId).username("user-" + userId)
                .subject("Asunto").description("Descripción")
                .status(status).createdAt(createdAt)
                .build();
    }

    @Test
    void createTicket_savesOpenTicketWithGivenIdentity() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateTicketRequestDTO request = new CreateTicketRequestDTO();
        request.setSubject("No abre el casillero");
        request.setDescription("El casillero 12 no abre");
        request.setRentalRef("Casillero #12 - Bloque A");

        TicketDTO result = ticketService.createTicket("1234567890", "jdoe", request);

        assertEquals("1234567890", result.getUserId());
        assertEquals("jdoe", result.getUsername());
        assertEquals(TicketStatus.OPEN, result.getStatus());
        assertEquals("Casillero #12 - Bloque A", result.getRentalRef());
    }

    @Test
    void getAll_ordersOpenTicketsBeforeClosed() {
        Instant now = Instant.now();
        Ticket closedNewer = ticket("1", "u1", TicketStatus.CLOSED, now);
        Ticket openOlder = ticket("2", "u2", TicketStatus.OPEN, now.minus(1, ChronoUnit.DAYS));
        when(ticketRepository.findAll()).thenReturn(List.of(closedNewer, openOlder));

        List<TicketDTO> result = ticketService.getAll();

        assertEquals("2", result.get(0).getId());
        assertEquals("1", result.get(1).getId());
    }

    @Test
    void getMine_ordersByCreatedAtDescending() {
        Instant now = Instant.now();
        Ticket older = ticket("1", "u1", TicketStatus.OPEN, now.minus(1, ChronoUnit.DAYS));
        Ticket newer = ticket("2", "u1", TicketStatus.OPEN, now);
        when(ticketRepository.findByUserId("u1")).thenReturn(List.of(older, newer));

        List<TicketDTO> result = ticketService.getMine("u1");

        assertEquals("2", result.get(0).getId());
        assertEquals("1", result.get(1).getId());
    }

    @Test
    void getMessages_ownerCanReadHistory() {
        Ticket t = ticket("1", "u1", TicketStatus.OPEN, Instant.now());
        when(ticketRepository.findById("1")).thenReturn(Optional.of(t));
        Message message = Message.builder().id("m1").ticketId("1").senderUsername("jdoe")
                .senderRole(SenderRole.USER).content("hola").sentAt(Instant.now()).build();
        when(messageRepository.findByTicketIdOrderBySentAtAsc("1")).thenReturn(List.of(message));

        List<MessageDTO> result = ticketService.getMessages("1", "u1", List.of("USER"));

        assertEquals(1, result.size());
        assertEquals("hola", result.get(0).getContent());
    }

    @Test
    void getMessages_adminCanReadAnyTicket() {
        Ticket t = ticket("1", "u1", TicketStatus.OPEN, Instant.now());
        when(ticketRepository.findById("1")).thenReturn(Optional.of(t));
        when(messageRepository.findByTicketIdOrderBySentAtAsc("1")).thenReturn(List.of());

        assertDoesNotThrow(() -> ticketService.getMessages("1", "admin-id", List.of("USER", "ADMIN")));
    }

    @Test
    void getMessages_strangerIsForbidden() {
        Ticket t = ticket("1", "u1", TicketStatus.OPEN, Instant.now());
        when(ticketRepository.findById("1")).thenReturn(Optional.of(t));

        assertThrows(ForbiddenException.class, () -> ticketService.getMessages("1", "u2", List.of("USER")));
    }

    @Test
    void getMessages_ticketNotFound_throwsNotFound() {
        when(ticketRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.getMessages("missing", "u1", List.of("USER")));
    }

    @Test
    void closeTicket_setsClosedStatusAndBroadcastsEvent() {
        Ticket t = ticket("1", "u1", TicketStatus.OPEN, Instant.now());
        when(ticketRepository.findById("1")).thenReturn(Optional.of(t));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketDTO result = ticketService.closeTicket("1", "admin");

        assertEquals(TicketStatus.CLOSED, result.getStatus());
        ArgumentCaptor<MessageDTO> captor = ArgumentCaptor.forClass(MessageDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/tickets/1"), captor.capture());
        assertTrue(captor.getValue().isTicketClosed());
    }

    @Test
    void sendMessage_onOpenTicket_persistsAndBroadcasts() {
        Ticket t = ticket("1", "u1", TicketStatus.OPEN, Instant.now());
        when(ticketRepository.findById("1")).thenReturn(Optional.of(t));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId("m1");
            return m;
        });

        ticketService.sendMessage("1", "user-u1", "USER", "hola admin");

        verify(messageRepository).save(any(Message.class));
        ArgumentCaptor<MessageDTO> captor = ArgumentCaptor.forClass(MessageDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/tickets/1"), captor.capture());
        assertEquals("hola admin", captor.getValue().getContent());
        assertFalse(captor.getValue().isTicketClosed());
    }

    @Test
    void sendMessage_fromUserWhoDoesNotOwnTicket_isIgnored() {
        Ticket t = ticket("1", "u1", TicketStatus.OPEN, Instant.now());
        when(ticketRepository.findById("1")).thenReturn(Optional.of(t));

        ticketService.sendMessage("1", "otro-usuario", "USER", "no soy el dueño");

        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/tickets/1"), any(MessageDTO.class));
    }

    @Test
    void sendMessage_onClosedTicket_isIgnored() {
        Ticket t = ticket("1", "u1", TicketStatus.CLOSED, Instant.now());
        when(ticketRepository.findById("1")).thenReturn(Optional.of(t));

        ticketService.sendMessage("1", "jdoe", "USER", "no debería enviarse");

        verify(messageRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
