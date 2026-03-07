package fr.xephi.authme.mail;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.FileUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.Files;
/**
 * Creates emails and sends them.
 */
public class EmailService {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(EmailService.class);

    private final File dataFolder;
    private final Settings settings;
    private final SendMailSsl sendMailSsl;

    @Inject
    EmailService(@DataFolder File dataFolder, Settings settings, SendMailSsl sendMailSsl) {
        this.dataFolder = dataFolder;
        this.settings = settings;
        this.sendMailSsl = sendMailSsl;
    }

    public boolean hasAllInformation() {
        return sendMailSsl.hasAllInformation();
    }


    /**
     * Sends an email to the user with his new password.
     *
     * @param name the name of the player
     * @param mailAddress the player's email
     * @param newPass the new password
     * @return true if email could be sent, false otherwise
     */
    public boolean sendPasswordMail(String name, String mailAddress, String newPass) {
        if (!hasAllInformation()) {
            logger.warning("Cannot perform email registration: not all email settings are complete");
            return false;
        }

        HtmlEmail email;
        try {
            email = sendMailSsl.initializeMail(mailAddress);
        } catch (EmailException e) {
            logger.logException("Failed to create email with the given settings:", e);
            return false;
        }

        String mailText = replaceTagsForPasswordMail(settings.getPasswordEmailMessage(), name, newPass);
        // Generate an image?
        File file = null;
        if (settings.getProperty(EmailSettings.PASSWORD_AS_IMAGE)) {
            try {
                file = generatePasswordImage(name, newPass);
                mailText = embedImageIntoEmailContent(file, email, mailText);
            } catch (IOException | EmailException e) {
                logger.logException(
                    "Unable to send new password as image for email " + mailAddress + ":", e);
            }
        }

        boolean couldSendEmail = sendMailSsl.sendEmail(mailText, email);
        FileUtils.delete(file);
        return couldSendEmail;
    }

    /**
     * Sends an email to the user with the temporary verification code.
     *
     * @param name the name of the player
     * @param mailAddress the player's email
     * @param code the verification code
     * @return true if email could be sent, false otherwise
     */
    public boolean sendVerificationMail(String name, String mailAddress, String code) {
        if (!hasAllInformation()) {
            logger.warning("Cannot send verification email: not all email settings are complete");
            return false;
        }

        HtmlEmail email;
        try {
            email = sendMailSsl.initializeMail(mailAddress);
        } catch (EmailException e) {
            logger.logException("Failed to create verification email with the given settings:", e);
            return false;
        }

        String mailText = replaceTagsForVerificationEmail(settings.getVerificationEmailMessage(), name, code,
            settings.getProperty(SecuritySettings.VERIFICATION_CODE_EXPIRATION_MINUTES));
        mailText = embedVerificationImage(mailText, email);

        String mailTextPlain = loadVerificationPlainText();
        if (!mailTextPlain.isEmpty()) {
            mailTextPlain = replaceTagsForVerificationEmail(mailTextPlain, name, code,
                settings.getProperty(SecuritySettings.VERIFICATION_CODE_EXPIRATION_MINUTES));
        }

        return sendMailSsl.sendEmail(mailText, mailTextPlain.isEmpty() ? mailText : mailTextPlain, email);
    }

    /**
     * Sends an email to the user with a recovery code for the password recovery process.
     *
     * @param name the name of the player
     * @param email the player's email address
     * @param code the recovery code
     * @return true if email could be sent, false otherwise
     */
    public boolean sendRecoveryCode(String name, String email, String code) {
        HtmlEmail htmlEmail;
        try {
            htmlEmail = sendMailSsl.initializeMail(email);
        } catch (EmailException e) {
            logger.logException("Failed to create email for recovery code:", e);
            return false;
        }

        String mailText = replaceTagsForRecoveryCodeMail(settings.getRecoveryCodeEmailMessage(),
            name, code, settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID));
        mailText = embedRecoveryImage(mailText, htmlEmail);

        String mailTextPlain = loadRecoveryPlainText();
        if (!mailTextPlain.isEmpty()) {
            mailTextPlain = replaceTagsForRecoveryCodeMail(mailTextPlain, name, code,
                settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID));
        }

        return sendMailSsl.sendEmail(mailText, mailTextPlain.isEmpty() ? mailText : mailTextPlain, htmlEmail);
    }

    private File generatePasswordImage(String name, String newPass) throws IOException {
        ImageGenerator gen = new ImageGenerator(newPass);
        File file = new File(dataFolder, name + "_new_pass.jpg");
        ImageIO.write(gen.generateImage(), "jpg", file);
        return file;
    }

    private String embedVerificationImage(String mailText, HtmlEmail email) {
        File imageFile = new File(new File(dataFolder, "verification code email"),
            "images/8f4bcb2edd0c0bfcc163493e4f73abe3.jpg");

        FileUtils.copyFileFromResource(imageFile,
            "verification code email/images/8f4bcb2edd0c0bfcc163493e4f73abe3.jpg");

        if (!imageFile.exists()) {
            return mailText;
        }

        try {
            String tag = email.embed(new FileDataSource(imageFile), imageFile.getName());
            return mailText.replace("images/8f4bcb2edd0c0bfcc163493e4f73abe3.jpg", "cid:" + tag);
        } catch (EmailException e) {
            logger.logException("Unable to embed verification email image:", e);
            return mailText;
        }
    }

    private String loadVerificationPlainText() {
        File txtFile = new File(new File(dataFolder, "verification code email"), "email.txt");
        FileUtils.copyFileFromResource(txtFile, "verification code email/email.txt");

        if (!txtFile.exists()) {
            return "";
        }

        try {
            return Files.asCharSource(txtFile, StandardCharsets.UTF_8).read();
        } catch (IOException e) {
            logger.logException("Failed to read verification plain text email:", e);
            return "";
        }
    }

    private String embedRecoveryImage(String mailText, HtmlEmail email) {
        File imageFile = new File(new File(dataFolder, "recovery code email"),
            "images/8f4bcb2edd0c0bfcc163493e4f73abe3.jpg");

        FileUtils.copyFileFromResource(imageFile,
            "recovery code email/images/8f4bcb2edd0c0bfcc163493e4f73abe3.jpg");

        if (!imageFile.exists()) {
            return mailText;
        }

        try {
            String tag = email.embed(new FileDataSource(imageFile), imageFile.getName());
            return mailText.replace("images/8f4bcb2edd0c0bfcc163493e4f73abe3.jpg", "cid:" + tag);
        } catch (EmailException e) {
            logger.logException("Unable to embed recovery email image:", e);
            return mailText;
        }
    }

    private String loadRecoveryPlainText() {
        File txtFile = new File(new File(dataFolder, "recovery code email"), "email.txt");
        FileUtils.copyFileFromResource(txtFile, "recovery code email/email.txt");

        if (!txtFile.exists()) {
            return "";
        }

        try {
            return Files.asCharSource(txtFile, StandardCharsets.UTF_8).read();
        } catch (IOException e) {
            logger.logException("Failed to read recovery plain text email:", e);
            return "";
        }
    }

    private static String embedImageIntoEmailContent(File image, HtmlEmail email, String content)
        throws EmailException {
        DataSource source = new FileDataSource(image);
        String tag = email.embed(source, image.getName());
        return content.replace("<image />", "<img src=\"cid:" + tag + "\">");
    }

    private String replaceTagsForPasswordMail(String mailText, String name, String newPass) {
        return mailText
            .replace("<playername />", name)
            .replace("<servername />", settings.getProperty(PluginSettings.SERVER_NAME))
            .replace("<generatedpass />", newPass);
    }

    private String replaceTagsForVerificationEmail(String mailText, String name, String code, int minutesValid) {
        return mailText
            .replace("<playername />", name)
            .replace("<servername />", settings.getProperty(PluginSettings.SERVER_NAME))
            .replace("<generatedcode />", code)
            .replace("<minutesvalid />", String.valueOf(minutesValid));
    }

    private String replaceTagsForRecoveryCodeMail(String mailText, String name, String code, int hoursValid) {
        if (mailText == null) {
            return "";
        }
        return replacePlaceholder(mailText, "playername", name)
            .replaceAll("(?i)<\\s*servername\\s*/?>", Matcher.quoteReplacement(settings.getProperty(PluginSettings.SERVER_NAME)))
            .replaceAll("(?i)<\\s*recoverycode\\s*/?>", Matcher.quoteReplacement(code))
            .replaceAll("(?i)<\\s*hoursvalid\\s*/?>", Matcher.quoteReplacement(String.valueOf(hoursValid)));
    }

    private String replacePlaceholder(String text, String key, String value) {
        String pattern = "(?i)<\\s*" + Pattern.quote(key) + "\\s*/?>";
        return text.replaceAll(pattern, Matcher.quoteReplacement(value));
    }
}
