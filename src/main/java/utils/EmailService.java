package utils;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * Service d'envoi d'emails pour la réinitialisation du mot de passe.
 *
 * ⚠️ CONFIGURATION OBLIGATOIRE :
 *   1. Remplacez EMAIL_EXPEDITEUR par votre adresse Gmail.
 *   2. Remplacez MOT_DE_PASSE_APP par un "Mot de passe d'application" Gmail.
 *      (Activez la validation en 2 étapes sur votre compte Gmail,
 *       puis allez dans : Compte Google → Sécurité → Mots de passe des applications)
 *   Ne jamais utiliser votre mot de passe Gmail principal ici.
 */
public class EmailService {

    // ====================================================================
    // ✅ MODIFIEZ UNIQUEMENT CES DEUX CONSTANTES
    // ====================================================================
    private static final String EMAIL_EXPEDITEUR = "omar.oueslati2009@gmail.com";
    private static final String MOT_DE_PASSE_APP = "sunb orbq fmqy gvjj"; // mot de passe d'application Gmail (16 caractères)
    // ====================================================================

    /**
     * Envoie un email contenant le code de vérification à l'utilisateur.
     *
     * @param destinataire  Adresse email du destinataire
     * @param code          Code à 6 chiffres généré
     * @return true si l'envoi a réussi, false sinon
     */
    public static boolean envoyerCodeReset(String destinataire, String code) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_EXPEDITEUR, MOT_DE_PASSE_APP);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_EXPEDITEUR, "Harmony - Sécurité"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject("🔐 Code de vérification Harmony");

            // Corps de l'email en HTML
            String contenuHTML = buildEmailHTML(code);
            message.setContent(contenuHTML, "text/html; charset=UTF-8");

            Transport.send(message);
            System.out.println("Email envoyé avec succès à : " + destinataire);
            return true;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Construit le corps HTML de l'email.
     */
    private static String buildEmailHTML(String code) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center" style="padding:40px 0;">
                        <table width="480" cellpadding="0" cellspacing="0"
                               style="background:white;border-radius:16px;overflow:hidden;
                                      box-shadow:0 4px 20px rgba(0,0,0,0.1);">
                
                          <!-- En-tête violet -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#667eea,#764ba2);
                                       padding:35px 40px;text-align:center;">
                              <h1 style="margin:0;color:white;font-size:28px;font-weight:bold;">
                                🔐 Harmony
                              </h1>
                              <p style="margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:15px;">
                                Réinitialisation de mot de passe
                              </p>
                            </td>
                          </tr>
                
                          <!-- Corps -->
                          <tr>
                            <td style="padding:40px;">
                              <p style="font-size:16px;color:#374151;margin:0 0 20px;">
                                Bonjour,
                              </p>
                              <p style="font-size:15px;color:#6B7280;margin:0 0 30px;">
                                Vous avez demandé à réinitialiser votre mot de passe.<br>
                                Entrez le code ci-dessous dans l'application :
                              </p>
                
                              <!-- Code -->
                              <div style="text-align:center;margin:0 0 30px;">
                                <div style="display:inline-block;background:#F3F4F6;
                                            border:2px dashed #8B5CF6;border-radius:12px;
                                            padding:18px 40px;">
                                  <span style="font-size:38px;font-weight:bold;
                                               letter-spacing:10px;color:#7C3AED;">
                                    %s
                                  </span>
                                </div>
                              </div>
                
                              <p style="font-size:13px;color:#9CA3AF;margin:0 0 10px;text-align:center;">
                                ⏱️ Ce code expire dans <strong>15 minutes</strong>.
                              </p>
                              <p style="font-size:13px;color:#9CA3AF;margin:0;text-align:center;">
                                Si vous n'avez pas fait cette demande, ignorez cet email.
                              </p>
                            </td>
                          </tr>
                
                          <!-- Pied de page -->
                          <tr>
                            <td style="background:#F9FAFB;padding:20px 40px;text-align:center;
                                       border-top:1px solid #E5E7EB;">
                              <p style="font-size:12px;color:#9CA3AF;margin:0;">
                                © 2024 Harmony — Plateforme de gestion étudiante
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(code);
    }
}
