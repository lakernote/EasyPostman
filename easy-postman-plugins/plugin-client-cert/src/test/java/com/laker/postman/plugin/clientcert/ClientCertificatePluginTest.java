package com.laker.postman.plugin.clientcert;

import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginDescriptor;
import com.laker.postman.plugin.api.PluginSettingsContribution;
import com.laker.postman.plugin.api.PluginSettingsContributionContext;
import com.laker.postman.plugin.api.ScriptCompletionContributor;
import com.laker.postman.plugin.api.SnippetDefinition;
import com.laker.postman.plugin.api.ToolboxContribution;
import com.laker.postman.plugin.api.service.ClientCertificatePluginService;
import org.testng.annotations.Test;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ClientCertificatePluginTest {

    @Test
    public void shouldRegisterServiceAndSettingsContribution() {
        CapturingPluginContext context = new CapturingPluginContext();

        new ClientCertificatePlugin().onLoad(context);

        assertNotNull(context.clientCertificateService);
        assertEquals(context.settingsContributions.size(), 1);

        PluginSettingsContribution contribution = context.settingsContributions.get(0);
        assertEquals(contribution.id(), "client-certificates");
        assertEquals(contribution.titleKey(), MessageKeys.CERT_TITLE);
        assertEquals(contribution.order(), 700);
        assertEquals(contribution.category(), PluginSettingsContribution.CATEGORY_EXTENSIONS);
        assertEquals(contribution.titleBundleName(), ClientCertI18n.BUNDLE_NAME);
        assertNotNull(contribution.titleClassLoader());
        assertTrue(contribution.createPanel(new PluginSettingsContributionContext(null)) instanceof JComponent);
    }

    private static final class CapturingPluginContext implements PluginContext {
        private ClientCertificatePluginService clientCertificateService;
        private final List<PluginSettingsContribution> settingsContributions = new ArrayList<>();

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor(
                    "plugin-client-cert",
                    "Client Certificate Plugin",
                    "1.0.0",
                    ClientCertificatePlugin.class.getName(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            );
        }

        @Override
        public void registerScriptApi(String alias, Supplier<Object> factory) {
        }

        @Override
        public <T> void registerService(Class<T> type, T service) {
            if (ClientCertificatePluginService.class.equals(type)) {
                clientCertificateService = ClientCertificatePluginService.class.cast(service);
            }
        }

        @Override
        public void registerToolboxContribution(ToolboxContribution contribution) {
        }

        @Override
        public void registerSettingsContribution(PluginSettingsContribution contribution) {
            settingsContributions.add(contribution);
        }

        @Override
        public void registerScriptCompletionContributor(ScriptCompletionContributor contributor) {
        }

        @Override
        public void registerSnippet(SnippetDefinition definition) {
        }
    }
}
