package com.magadhexplora.api.mail;

import com.magadhexplora.api.config.MailProperties;
import com.magadhexplora.api.lead.booking.BookingEntity;
import com.magadhexplora.api.lead.contact.ContactEntity;
import com.magadhexplora.api.lead.quote.QuoteEntity;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MailProperties props;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine, MailProperties props) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.props = props;
    }

    @Async("mailExecutor")
    public void sendContactEmails(ContactEntity c) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("contact", c);
        ctx.put("brand", props.getBrand());

        send(c.getEmail(), "We received your message — " + props.getBrand(), "mail/contact-customer", ctx);
        send(props.getAdminTo(), "New contact: " + c.getName(), "mail/contact-admin", ctx);
    }

    @Async("mailExecutor")
    public void sendQuoteEmails(QuoteEntity q) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("quote", q);
        ctx.put("brand", props.getBrand());

        send(q.getEmail(), "Your quote request — " + props.getBrand(), "mail/quote-customer", ctx);
        send(props.getAdminTo(), "New quote: " + q.getName(), "mail/quote-admin", ctx);
    }

    @Async("mailExecutor")
    public void sendBookingEmails(BookingEntity b) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("booking", b);
        ctx.put("brand", props.getBrand());

        send(b.getEmail(), "Booking received — " + props.getBrand(), "mail/booking-customer", ctx);
        send(props.getAdminTo(), "New booking: " + b.getName(), "mail/booking-admin", ctx);
    }

    private void send(String to, String subject, String template, Map<String, Object> model) {
        try {
            Context ctx = new Context();
            ctx.setVariables(model);
            String html = templateEngine.process(template, ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
            h.setFrom(props.getFrom());
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            mailSender.send(msg);
            log.info("Mail sent to {} subject='{}'", to, subject);
        } catch (Exception ex) {
            log.warn("Mail send failed to {} ({}): {}", to, subject, ex.toString());
        }
    }
}
