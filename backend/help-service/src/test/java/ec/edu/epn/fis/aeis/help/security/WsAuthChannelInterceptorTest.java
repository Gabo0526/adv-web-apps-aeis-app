package ec.edu.epn.fis.aeis.help.security;

import ec.edu.epn.fis.aeis.help.model.document.Ticket;
import ec.edu.epn.fis.aeis.help.model.enums.TicketStatus;
import ec.edu.epn.fis.aeis.help.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WsAuthChannelInterceptorTest {

    @Mock
    private TicketRepository ticketRepository;

    private WsAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WsAuthChannelInterceptor(ticketRepository);
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination, String username, String role) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        Map<String, Object> attributes = new HashMap<>();
        if (username != null) {
            attributes.put("username", username);
            attributes.put("role", role);
        }
        accessor.setSessionAttributes(attributes);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Ticket ticketOwnedBy(String username) {
        return Ticket.builder()
                .id("t1")
                .userId("1717171717")
                .username(username)
                .subject("ayuda")
                .status(TicketStatus.OPEN)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void rejectsSendDirectlyToTopic() {
        Message<byte[]> msg = stompMessage(StompCommand.SEND, "/topic/tickets/t1", "user1", "USER");
        assertThatThrownBy(() -> interceptor.preSend(msg, null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void allowsSendToApplicationDestination() {
        Message<byte[]> msg = stompMessage(StompCommand.SEND, "/app/tickets/t1/send", "user1", "USER");
        assertThatCode(() -> interceptor.preSend(msg, null)).doesNotThrowAnyException();
    }

    @Test
    void allowsOwnerToSubscribeToOwnTicket() {
        when(ticketRepository.findById("t1")).thenReturn(Optional.of(ticketOwnedBy("user1")));
        Message<byte[]> msg = stompMessage(StompCommand.SUBSCRIBE, "/topic/tickets/t1", "user1", "USER");
        assertThatCode(() -> interceptor.preSend(msg, null)).doesNotThrowAnyException();
    }

    @Test
    void rejectsSubscribeToForeignTicket() {
        when(ticketRepository.findById("t1")).thenReturn(Optional.of(ticketOwnedBy("otro")));
        Message<byte[]> msg = stompMessage(StompCommand.SUBSCRIBE, "/topic/tickets/t1", "user1", "USER");
        assertThatThrownBy(() -> interceptor.preSend(msg, null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void allowsAdminToSubscribeToAnyTicket() {
        Message<byte[]> msg = stompMessage(StompCommand.SUBSCRIBE, "/topic/tickets/t1", "admin", "ADMIN");
        assertThatCode(() -> interceptor.preSend(msg, null)).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnauthenticatedSubscribeToTicketTopic() {
        Message<byte[]> msg = stompMessage(StompCommand.SUBSCRIBE, "/topic/tickets/t1", null, null);
        assertThatThrownBy(() -> interceptor.preSend(msg, null))
                .isInstanceOf(MessagingException.class);
    }
}
