package ec.edu.epn.fis.aeis.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void givenValidEmailData_whenSendEmail_thenSendsEmailCorrectly() {
        String to = "test@example.com";
        String subject = "Test Subject";
        String text = "Test message content";

        emailService.sendEmail(to, subject, text);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertArrayEquals(new String[]{to}, capturedMessage.getTo());
        assertEquals(subject, capturedMessage.getSubject());
        assertEquals(text, capturedMessage.getText());
    }

    @Test
    void givenMultipleRecipients_whenSendEmail_thenSendsToCorrectRecipient() {
        String to = "user@domain.com";
        String subject = "Important Notice";
        String text = "This is an important message";

        emailService.sendEmail(to, subject, text);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertArrayEquals(new String[]{to}, capturedMessage.getTo());
        assertEquals(subject, capturedMessage.getSubject());
        assertEquals(text, capturedMessage.getText());
    }

    @Test
    void givenEmptySubject_whenSendEmail_thenSendsEmailWithEmptySubject() {
        String to = "test@example.com";
        String subject = "";
        String text = "Message with empty subject";

        emailService.sendEmail(to, subject, text);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertEquals("", capturedMessage.getSubject());
        assertEquals(text, capturedMessage.getText());
    }

    @Test
    void givenEmptyText_whenSendEmail_thenSendsEmailWithEmptyText() {
        String to = "test@example.com";
        String subject = "Subject without content";
        String text = "";

        emailService.sendEmail(to, subject, text);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertEquals(subject, capturedMessage.getSubject());
        assertEquals("", capturedMessage.getText());
    }

    @Test
    void givenLongEmailContent_whenSendEmail_thenHandlesLongContentCorrectly() {
        String to = "longcontent@example.com";
        String subject = "Very Long Subject That Might Exceed Normal Length Limits For Email Subjects";
        String text = "This is a very long email message that contains multiple lines and extensive content. ".repeat(50);

        emailService.sendEmail(to, subject, text);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertArrayEquals(new String[]{to}, capturedMessage.getTo());
        assertEquals(subject, capturedMessage.getSubject());
        assertEquals(text, capturedMessage.getText());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void givenSpecialCharacters_whenSendEmail_thenHandlesSpecialCharactersCorrectly() {
        String to = "special@example.com";
        String subject = "Título con acentos y ñ - Special chars: @#$%^&*()";
        String text = "Mensaje con caracteres especiales: áéíóú ñÑ ¿¡ €$£¥ 中文 日本語 русский";

        emailService.sendEmail(to, subject, text);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertArrayEquals(new String[]{to}, capturedMessage.getTo());
        assertEquals(subject, capturedMessage.getSubject());
        assertEquals(text, capturedMessage.getText());
    }
}
