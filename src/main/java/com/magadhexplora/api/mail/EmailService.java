package com.magadhexplora.api.mail;

import com.magadhexplora.api.catalog.pkg.PackageEntity;
import com.magadhexplora.api.catalog.pkg.PackageRepository;
import com.magadhexplora.api.config.MailProperties;
import com.magadhexplora.api.config.SiteProperties;
import com.magadhexplora.api.lead.abandoned.AbandonedLeadEntity;
import com.magadhexplora.api.lead.booking.BookingEntity;
import com.magadhexplora.api.lead.contact.ContactEntity;
import com.magadhexplora.api.lead.quote.QuoteEntity;
import com.magadhexplora.api.pdf.PackagePdfService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
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
    private final SiteProperties siteProps;
    private final PackagePdfService packagePdfService;
    private final PackageRepository packageRepository;

    public EmailService(JavaMailSender mailSender,
                        SpringTemplateEngine templateEngine,
                        MailProperties props,
                        SiteProperties siteProps,
                        PackagePdfService packagePdfService,
                        PackageRepository packageRepository) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.props = props;
        this.siteProps = siteProps;
        this.packagePdfService = packagePdfService;
        this.packageRepository = packageRepository;
    }

    @Async("mailExecutor")
    public void sendContactEmails(ContactEntity c) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("contact", c);
        ctx.put("brand", props.getBrand());

        send(c.getEmail(), "We received your message — " + props.getBrand(), "mail/contact-customer", ctx, null);
        send(props.getAdminTo(), "New contact: " + c.getName(), "mail/contact-admin", ctx, null);
    }

    @Async("mailExecutor")
    public void sendQuoteEmails(QuoteEntity q) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("quote", q);
        ctx.put("brand", props.getBrand());

        Attachment brochure = renderBrochureSafely(q.getPackageId());

        send(q.getEmail(), "Your quote request — " + props.getBrand(), "mail/quote-customer", ctx, brochure);
        send(props.getAdminTo(), "New quote: " + q.getName(), "mail/quote-admin", ctx, brochure);
    }

    @Async("mailExecutor")
    public void sendAbandonedLeadNotification(AbandonedLeadEntity lead) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("lead", lead);
        ctx.put("brand", props.getBrand());
        ctx.put("adminUrl", siteProps.publicUrlClean());

        send(props.getAdminTo(),
                "Abandoned lead: " + (lead.getName() == null || lead.getName().isBlank()
                        ? lead.getEmail() : lead.getName()),
                "mail/abandoned-lead-admin", ctx, null);
    }

    /** Send the next 3-touch recovery email (touch = 1, 2, or 3). */
    @Async("mailExecutor")
    public void sendRecoveryTouch(AbandonedLeadEntity lead, int touch) {
        if (lead.getEmail() == null || lead.getEmail().isBlank()) return;
        if (touch < 1 || touch > 3) return;

        String recoveryUrl = siteProps.publicUrlClean() + "/r/" + lead.getRecoveryToken();

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("lead", lead);
        ctx.put("brand", props.getBrand());
        ctx.put("recoveryUrl", recoveryUrl);
        ctx.put("siteUrl", siteProps.publicUrlClean());

        String subject;
        String template;
        switch (touch) {
            case 1 -> {
                subject = "We saved your enquiry — " + props.getBrand();
                template = "mail/recovery-touch1";
            }
            case 2 -> {
                subject = "Still thinking about your Bihar trip?";
                template = "mail/recovery-touch2";
            }
            default -> {
                subject = "Last chance to complete your enquiry";
                template = "mail/recovery-touch3";
            }
        }
        send(lead.getEmail(), subject, template, ctx, null);
    }

    @Async("mailExecutor")
    public void sendBookingCancellation(BookingEntity b) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("booking", b);
        ctx.put("brand", props.getBrand());
        ctx.put("siteUrl", siteProps.publicUrlClean());
        ctx.put("viewBookingUrl",
                siteProps.publicUrlClean() + "/booking/" + (b.getViewToken() == null ? "" : b.getViewToken()));

        send(b.getEmail(),
                "Booking cancelled — " + props.getBrand() + " #" + b.getId(),
                "mail/booking-cancelled", ctx, null);
        send(props.getAdminTo(),
                "Cancelled: " + b.getName() + " #" + b.getId(),
                "mail/booking-cancelled", ctx, null);
    }

    @Async("mailExecutor")
    public void sendBookingEmails(BookingEntity b) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("booking", b);
        ctx.put("brand", props.getBrand());
        ctx.put("siteUrl", siteProps.publicUrlClean());
        ctx.put("viewBookingUrl",
                siteProps.publicUrlClean() + "/booking/" + (b.getViewToken() == null ? "" : b.getViewToken()));

        Attachment brochure = renderBrochureSafely(b.getPackageId());

        send(b.getEmail(), "Booking received — " + props.getBrand(), "mail/booking-customer", ctx, brochure);
        send(props.getAdminTo(), "New booking: " + b.getName(), "mail/booking-admin", ctx, brochure);
    }

    private Attachment renderBrochureSafely(Long packageId) {
        if (packageId == null) return null;
        try {
            PackageEntity pkg = packageRepository.findById(packageId).orElse(null);
            if (pkg == null) return null;
            byte[] bytes = packagePdfService.render(pkg);
            String safeSlug = pkg.getSlug() == null
                    ? ("package-" + pkg.getId())
                    : pkg.getSlug().replaceAll("[^a-zA-Z0-9-_]", "-");
            return new Attachment("Magadh-" + safeSlug + ".pdf", bytes);
        } catch (Exception ex) {
            log.warn("Skipping brochure attachment for package {}: {}", packageId, ex.toString());
            return null;
        }
    }

    /**
     * Fire-and-forget admin alert. Plain-HTML body so we don't need a Thymeleaf
     * template per alert rule. Safe to call from scheduled jobs.
     */
    @Async("mailExecutor")
    public void sendAdminAlert(String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, false, StandardCharsets.UTF_8.name());
            h.setFrom(props.getFrom());
            h.setTo(props.getAdminTo());
            h.setSubject("[" + props.getBrand() + " Alert] " + subject);
            h.setText(htmlBody, true);
            mailSender.send(msg);
            log.info("Admin alert mail sent: {}", subject);
        } catch (Exception ex) {
            log.warn("Admin alert mail failed ({}): {}", subject, ex.toString());
        }
    }

    private void send(String to, String subject, String template,
                      Map<String, Object> model, Attachment attachment) {
        try {
            Context ctx = new Context();
            ctx.setVariables(model);
            String html = templateEngine.process(template, ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            boolean multipart = attachment != null;
            MimeMessageHelper h = new MimeMessageHelper(msg, multipart, StandardCharsets.UTF_8.name());
            h.setFrom(props.getFrom());
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            if (attachment != null) {
                h.addAttachment(attachment.filename(), new ByteArrayResource(attachment.bytes()));
            }
            mailSender.send(msg);
            log.info("Mail sent to {} subject='{}'{}", to, subject,
                    attachment != null ? " (with brochure)" : "");
        } catch (Exception ex) {
            log.warn("Mail send failed to {} ({}): {}", to, subject, ex.toString());
        }
    }

    private record Attachment(String filename, byte[] bytes) {}
}
