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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TicketService(TicketRepository ticketRepository, MessageRepository messageRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public TicketDTO createTicket(String userId, String username, CreateTicketRequestDTO request) {
        Ticket ticket = Ticket.builder()
                .userId(userId)
                .username(username)
                .subject(request.getSubject())
                .description(request.getDescription())
                .rentalRef(request.getRentalRef())
                .status(TicketStatus.OPEN)
                .createdAt(Instant.now())
                .build();
        return new TicketDTO(ticketRepository.save(ticket));
    }

    public List<TicketDTO> getMine(String userId) {
        return ticketRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt).reversed())
                .map(TicketDTO::new)
                .toList();
    }

    public List<TicketDTO> getAll() {
        return ticketRepository.findAll().stream()
                .sorted(Comparator.<Ticket>comparingInt(t -> t.getStatus() == TicketStatus.OPEN ? 0 : 1)
                        .thenComparing(Comparator.comparing(Ticket::getCreatedAt).reversed()))
                .map(TicketDTO::new)
                .toList();
    }

    public List<MessageDTO> getMessages(String ticketId, String requesterId, List<String> requesterRoles) {
        Ticket ticket = getTicketOrThrow(ticketId);
        assertOwnerOrAdmin(ticket, requesterId, requesterRoles);
        return messageRepository.findByTicketIdOrderBySentAtAsc(ticketId).stream()
                .map(MessageDTO::new)
                .toList();
    }

    public TicketDTO closeTicket(String ticketId, String closedByUsername) {
        Ticket ticket = getTicketOrThrow(ticketId);
        ticket.setStatus(TicketStatus.CLOSED);
        Ticket saved = ticketRepository.save(ticket);
        messagingTemplate.convertAndSend("/topic/tickets/" + ticketId, MessageDTO.closedEvent(ticketId, closedByUsername));
        return new TicketDTO(saved);
    }

    public void sendMessage(String ticketId, String senderUsername, String senderRoleStr, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        Ticket ticket = getTicketOrThrow(ticketId);
        if (ticket.getStatus() != TicketStatus.OPEN) {
            return;
        }

        SenderRole senderRole = "ADMIN".equals(senderRoleStr) ? SenderRole.ADMIN : SenderRole.USER;
        if (senderRole != SenderRole.ADMIN && !ticket.getUsername().equals(senderUsername)) {
            return;
        }
        Message message = Message.builder()
                .ticketId(ticketId)
                .senderUsername(senderUsername)
                .senderRole(senderRole)
                .content(content)
                .sentAt(Instant.now())
                .build();
        Message saved = messageRepository.save(message);
        messagingTemplate.convertAndSend("/topic/tickets/" + ticketId, new MessageDTO(saved));
    }

    private Ticket getTicketOrThrow(String ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));
    }

    private void assertOwnerOrAdmin(Ticket ticket, String requesterId, List<String> requesterRoles) {
        boolean isAdmin = requesterRoles != null && requesterRoles.contains("ADMIN");
        boolean isOwner = ticket.getUserId().equals(requesterId);
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("No tienes acceso a este ticket");
        }
    }
}
