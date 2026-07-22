package ec.edu.epn.fis.aeis.help.controller;

import ec.edu.epn.fis.aeis.help.dto.ChatMessageRequestDTO;
import ec.edu.epn.fis.aeis.help.service.TicketService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * Cliente envía a /app/tickets/{ticketId}/send; el servidor persiste el
 * mensaje (identidad tomada del token, guardada en la sesión WS por
 * JwtHandshakeInterceptor) y lo publica en /topic/tickets/{ticketId}
 * (ver PLAN.md §7.3).
 */
@Controller
public class ChatController {

    private final TicketService ticketService;

    public ChatController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @MessageMapping("/tickets/{ticketId}/send")
    public void send(@DestinationVariable String ticketId,
                      ChatMessageRequestDTO payload,
                      SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return;
        }
        String username = (String) sessionAttributes.get("username");
        String role = (String) sessionAttributes.get("role");
        if (username == null) {
            return;
        }
        ticketService.sendMessage(ticketId, username, role, payload.getContent());
    }
}
