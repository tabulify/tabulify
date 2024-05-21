package net.bytle.email;


import net.bytle.exception.CastException;
import net.bytle.html.CssInliner;
import net.bytle.template.api.TemplateEngine;
import net.bytle.type.Casts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The email designer helps to write transactional email
 * where the user needs to click on a link.
 * <p>
 * The body is just the action.
 * <p>
 * Inspiration
 * <a href="https://github.com/eladnava/ma<a href="ilgen">...</a>
 * ">* https://www.thymeleaf.org/doc/articl</a>es/springmail.html (code: <a href="https://github.com/thymeleaf/thymeleafexamples-springmail">...</a>)
 */
public class BMailTransactionalTemplate {


  public static final String DEFAULT_TEMPLATE_NAME = "letter";
  private final TemplateEngine templateEngine;
  private final Map<String, Object> templateVariable = new HashMap<>();
  private final String templatePath;
  private final Brand brand;


  public BMailTransactionalTemplate(String templatePath, TemplateEngine templateEngine) {

    this.templatePath = templatePath;
    this.templateEngine = templateEngine;

    /**
     * Create the object
     */
    brand = Brand.create();
    this.templateVariable.put("brand", brand);
    this.templateVariable.put("primaryColor", "#3498db");

  }


  public static BMailTransactionalTemplate createFromPath(String templatePath) {
    return create(templatePath, BMailTransactionalLocalTemplateEngine.getDefault());
  }

  public static BMailTransactionalTemplate createFromName(String templateName) {
    return createFromName(templateName, BMailTransactionalLocalTemplateEngine.getDefault());
  }


  /**
   * Create a template with a text specialized for account creation
   *
   * @return the account creation template
   */
  public static BMailTransactionalTemplate createForAccountCreation() {
    return createFromDefault()
      .addIntroParagraph("We are glad you are here.")
      .addIntroParagraph("Press the button below to activate your account")
      .setActionName("Let's activate my account")
      .addOutroParagraph("Just to confirm you are you and to prevent any misuse of your email without your permission.");
  }

  private static BMailTransactionalTemplate createFromDefault() {
    return createFromName(DEFAULT_TEMPLATE_NAME);
  }

  public static BMailTransactionalTemplate create(String templatePath, TemplateEngine templateEngine) {
    return new BMailTransactionalTemplate(templatePath, templateEngine);
  }

  public static BMailTransactionalTemplate createFromName(String templateName, TemplateEngine templateEngine) {
    String templatePath = templateName + "/" + templateName;
    return create(templatePath, templateEngine);
  }

  public BMailTransactionalTemplate setActionName(String name) {
    getActionList().get(0).setActionName(name);
    return this;
  }


  /**
   * @param intro - a line before the call-to-action button
   * @return the object for chaining
   */
  public BMailTransactionalTemplate addIntroParagraph(String intro) {
    List<String> intros = Casts.castToListSafe(
      this.templateVariable.computeIfAbsent("intros", k -> new ArrayList<String>()),
      String.class
    );
    intros.add(intro);
    this.templateVariable.put("intros", intros);
    return this;
  }

  /**
   * @param name - the action name
   * @return the action to configure it further
   */
  public Action addAction(String name) {

    List<Action> actions = getActionList();
    Action action = new Action(this);
    action.setActionName(name);
    actions.add(action);
    return action;

  }

  private List<Action> getActionList() {

    Object actionsVariable = this.templateVariable.get("actions");
    if (actionsVariable == null) {
      List<Action> actions = new ArrayList<>();
      this.templateVariable.put("actions", actions);
      Action action = new Action(this);
      action.setActionName("A compelling CTA. i.e., NOT click here »");
      actions.add(action);
      return actions;
    }
    try {
      return Casts.castToList(actionsVariable, Action.class);
    } catch (CastException e) {
      throw new RuntimeException("The actions value (" + actionsVariable + ") of the template body could not be cast to an action");
    }
  }

  /**
   * An outro is also known as the closing.
   *
   * @param outro outro paragraph
   * @return the template
   */
  public BMailTransactionalTemplate addOutroParagraph(String outro) {
    List<String> outros = Casts.castToListSafe(
      this.templateVariable.computeIfAbsent("outros", k -> new ArrayList<String>()),
      String.class
    );
    outros.add(outro);
    this.templateVariable.put("outros", outros);
    return this;
  }

  public BMailTransactionalTemplate addClosingParagraph(String signature) {
    String closing = "closings";
    List<String> signatures = Casts.castToListSafe(
      this.templateVariable.computeIfAbsent(closing, k -> new ArrayList<String>()),
      String.class
    );
    signatures.add(signature);
    this.templateVariable.put(closing, signatures);
    return this;
  }

  public BMailTransactionalTemplate setSalutation(String salutation) {
    this.templateVariable.put("salutation", salutation);
    return this;
  }

  public BMailTransactionalTemplate setRecipientName(String recipientName) {
    this.templateVariable.put("recipientName", recipientName);
    return this;
  }

  /**
   * @param description is the preview that appears in the email list, not in the body
   * @return object for chaining
   */
  public BMailTransactionalTemplate setPreview(String description) {
    this.templateVariable.put("preview", description);
    return this;
  }

  public BMailTransactionalTemplate setBrandLogo(String logoUrl) {
    this.brand.setLogoUrl(logoUrl);
    return this;
  }

  public BMailTransactionalTemplate setBrandName(String name) {
    this.brand.setName(name);
    return this;
  }


  public BMailTransactionalTemplate setBrandLogoWidth(String width) {
    this.brand.setLogoWidth(width);
    return this;
  }

  /**
   * The title of the HTML page if the subject is not given
   *
   * @param title the email heading
   * @return object for chaining
   */
  public BMailTransactionalTemplate setTitle(String title) {
    this.templateVariable.put("title", title);
    return this;
  }

  public BMailTransactionalTemplate addPostScriptum(String postScriptum) {
    List<String> postScriptums = Casts.castToListSafe(
      this.templateVariable.computeIfAbsent("ps", k -> new ArrayList<String>()),
      String.class
    );
    postScriptums.add(postScriptum);
    this.templateVariable.put("ps", postScriptum);
    return this;
  }

  public BMailTransactionalTemplate setPrimaryColor(String primaryColor) {
    this.templateVariable.put("primaryColor", primaryColor);
    return this;
  }

  public BMailTransactionalTemplate setBrandUrl(String url) {
    this.brand.setUrl(url);
    return this;
  }

  public BMailTransactionalTemplate setActionUrl(String url) {
    getActionList().get(0).setUrl(url);
    return this;
  }

  public BMailTransactionalTemplate setActionDescription(String description) {
    getActionList().get(0).setActionDescription(description);
    return this;
  }

  public BMailTransactionalTemplate setActionIsGo(boolean b) {
    getActionList().get(0).setIsGoToAction(b);
    return this;
  }

  /**
   * @param valediction the valediction
   * @return the object for chaining
   * From Yours faithfully to Kind Regards
   */
  public BMailTransactionalTemplate setValediction(String valediction) {
    this.templateVariable.put("valediction", valediction);
    return this;
  }

  public BMailTransactionalTemplate setSenderName(String senderName) {
    this.templateVariable.put("senderName", senderName);
    return this;
  }

  public BMailTransactionalTemplate setDebug(Boolean debug) {
    this.templateVariable.put("debug", debug);
    return this;
  }

  public String generateHTMLForEmail() {
    return CssInliner
      .createFromStringDocument(this.generateHTML())
      .inline()
      .toString();
  }

  public BMailTransactionalTemplate setSenderAvatar(String senderAvatarUrl) {
    this.templateVariable.put("senderAvatar", senderAvatarUrl);
    return this;
  }

  public BMailTransactionalTemplate setSenderEmail(String email) {
    this.templateVariable.put("senderEmail", email);
    return this;
  }

  public BMailTransactionalTemplate setSenderTitle(String title) {
    this.templateVariable.put("senderTitle", title);
    return this;
  }

  public BMailTransactionalTemplate setBrandSlogan(String slogan) {
    this.templateVariable.put("brandSlogan", slogan);
    return this;
  }

  public BMailTransactionalTemplate setSenderFullName(String fullName) {
    this.templateVariable.put("senderFullName", fullName);
    return this;
  }


  /**
   * The action
   */
  static class Action {

    private final BMailTransactionalTemplate BMailTransactionalTemplate;

    /**
     * The name of the action is shown
     * in the text of the button
     */
    private String name;

    /**
     * The description is shown
     * in the title of a button or anchor
     * and as an introduction in the text template
     */
    private String description;

    /**
     * The a href link
     */
    private String url;

    public Action(BMailTransactionalTemplate BMailTransactionalTemplate) {
      this.BMailTransactionalTemplate = BMailTransactionalTemplate;
    }


    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    /**
     * The action link
     *
     * @return the url
     */
    public String getUrl() {
      return url;
    }

    /**
     * Description are shown in text template
     * and as title of the button or link
     *
     * @param description the action description
     * @return object for chaining
     */
    public Action setActionDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * The text of the button shown in the email
     * or in the shortcut `goToAction` button
     * in the mail list panel
     *
     * @param name the action name
     * @return object for chaining
     */
    public Action setActionName(String name) {
      this.name = name;
      return this;
    }

    /**
     * @return the root object for fluent building
     */
    public BMailTransactionalTemplate getEmailDesigner() {
      return BMailTransactionalTemplate;
    }

    /**
     * If true, a goToAction button will be created
     * in the mail list panel of Gmail
     * <a href="https://developers.google.com/gmail/markup/reference/go-to-action">...</a>
     *
     * @param b set if it's a go-to action button
     * @return object for chaining
     */
    public Action setIsGoToAction(boolean b) {
      if (b) {
        BMailTransactionalTemplate.templateVariable.put("goToAction", this);
      }
      return this;
    }


    public Action setUrl(String url) {
      this.url = url;
      return this;
    }
  }


  /**
   * Company / Brand / Sender Information
   * That is in the footer
   */
  static class Brand {

    private String name;
    private String logoWidth = "100%";
    private String logoUrl;
    private String url;
    private final List<String> postScriptum = new ArrayList<>();

    public String getLogoUrl() {
      return logoUrl;
    }

    public String getName() {
      return name;
    }


    public static Brand create() {
      return new Brand();
    }

    private Brand() {
    }

    public String getLogoWidth() {
      return logoWidth;
    }

    public void setLogoWidth(String logoWidth) {
      this.logoWidth = logoWidth;
    }

    public void setName(String name) {
      this.name = name;
    }


    public void setLogoUrl(String logoUrl) {
      this.logoUrl = logoUrl;
    }

    public String getUrl() {
      return url;
    }

    public Brand setUrl(String url) {
      this.url = url;
      return this;
    }


  }

  /**
   * @return the html
   */
  public String generateHTML() {

    return templateEngine.compile(this.templatePath + ".html")
      .applyVariables(templateVariable)
      .getResult();

  }

  public String generatePlainText() {

    String template = this.templatePath + ".txt";

    // We don't want any log as we know that the file may be not found
    // Log4j is used by thymeleaf
    // We use JDK log as log4j api implementation, we then
    // set the level off here with the JDK log api
    Logger logger = Logger.getLogger(org.thymeleaf.TemplateEngine.class.getName());
    Level oldLevel = logger.getLevel();
    try {
      logger.setLevel(Level.OFF);
      return templateEngine.compile(template)
        .applyVariables(templateVariable)
        .getResult();
    } catch (Exception e) {
      logger.setLevel(oldLevel);
      return BMailTransactionalLocalTemplateEngine
        .getDefault()
        .compile(DEFAULT_TEMPLATE_NAME + "/" + DEFAULT_TEMPLATE_NAME + ".txt")
        .applyVariables(templateVariable)
        .getResult();
    } finally {
      logger.setLevel(oldLevel);
    }


  }

}
