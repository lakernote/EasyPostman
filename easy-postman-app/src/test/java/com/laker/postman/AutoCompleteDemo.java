package com.laker.postman;

import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.*;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class AutoCompleteDemo extends JFrame {

   public AutoCompleteDemo() {

      JPanel contentPane = new JPanel(new BorderLayout());
      RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
      textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
      textArea.setCodeFoldingEnabled(true);
      contentPane.add(new RTextScrollPane(textArea));

      // A CompletionProvider is what knows of all possible completions, and
      // analyzes the contents of the text area at the caret position to
      // determine what completion choices should be presented. Most instances
      // of CompletionProvider (such as DefaultCompletionProvider) are designed
      // so that they can be shared among multiple text components.
      CompletionProvider provider = createCompletionProvider();

      // An AutoCompletion acts as a "middle-man" between a text component
      // and a CompletionProvider. It manages any options associated with
      // the auto-completion (the popup trigger key, whether to display a
      // documentation window along with completion choices, etc.). Unlike
      // CompletionProviders, instances of AutoCompletion cannot be shared
      // among multiple text components.
      AutoCompletion ac = new AutoCompletion(provider);
      ac.setAutoActivationEnabled(true);
      ac.setAutoCompleteEnabled(true);
      ac.setAutoActivationDelay(100);
      ac.setAutoCompleteSingleChoices(true);
      ac.setShowDescWindow(true);
      ac.setTriggerKey(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
      ac.install(textArea);

      setContentPane(contentPane);
      setTitle("AutoComplete Demo");
      setDefaultCloseOperation(EXIT_ON_CLOSE);
      pack();
      setLocationRelativeTo(null);

   }

   /**
    * Create a simple provider that adds some Java-related completions.
    */
   private CompletionProvider createCompletionProvider() {

      // A DefaultCompletionProvider is the simplest concrete implementation
      // of CompletionProvider. This provider has no understanding of
      // language semantics. It simply checks the text entered up to the
      // caret position for a match against known completions. This is all
      // that is needed in the majority of cases.
      DefaultCompletionProvider provider = new DefaultCompletionProvider();

      // Add completions for all Java keywords. A BasicCompletion is just
      // a straightforward word completion.
      provider.addCompletion(new BasicCompletion(provider, "abstract"));
      provider.addCompletion(new BasicCompletion(provider, "assert"));
      provider.addCompletion(new BasicCompletion(provider, "break"));
      provider.addCompletion(new BasicCompletion(provider, "case"));
      // ... etc ...
      provider.addCompletion(new BasicCompletion(provider, "transient"));
      provider.addCompletion(new BasicCompletion(provider, "try"));
      provider.addCompletion(new BasicCompletion(provider, "void"));
      provider.addCompletion(new BasicCompletion(provider, "volatile"));
      provider.addCompletion(new BasicCompletion(provider, "while"));

      // Add a couple of "shorthand" completions. These completions don't
      // require the input text to be the same thing as the replacement text.
      provider.addCompletion(new ShorthandCompletion(provider, "sysout",
            "System.out.println(", "System.out.println("));
      provider.addCompletion(new ShorthandCompletion(provider, "syserr",
            "System.err.println(", "System.err.println("));


      return provider;

   }

   public static void main(String[] args) {
      // Instantiate GUI on the EDT.
      SwingUtilities.invokeLater(() -> {
        try {
           String laf = UIManager.getSystemLookAndFeelClassName();
           UIManager.setLookAndFeel(laf);
        } catch (Exception e) { /* Never happens */ }
        new AutoCompleteDemo().setVisible(true);
      });
   }

}