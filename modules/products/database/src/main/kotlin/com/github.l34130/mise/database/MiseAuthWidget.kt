package com.github.l34130.mise.database

import com.intellij.database.dataSource.DatabaseAuthProvider
import com.intellij.database.dataSource.DatabaseConnectionConfig
import com.intellij.database.dataSource.DatabaseConnectionPoint
import com.intellij.database.dataSource.url.template.MutableParametersHolder
import com.intellij.database.dataSource.url.template.ParametersHolder
import com.intellij.ide.wizard.setMinimumWidthForAllRowLabels
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class MiseAuthWidget : DatabaseAuthProvider.AuthWidget {
    private val myConfig = MiseDatabaseAuthConfig()

    private val panel =
        panel {
            row("User:") {
                textField()
                    .bindText(myConfig::usernameKey)
                    .focused()
                    .applyToComponent {
                        emptyText.text = MiseDatabaseAuthConfig.PROP_USERNAME_KEY_DEFAULT
                    }
            }.layout(RowLayout.PARENT_GRID)
            row("Password:") {
                textField()
                    .bindText(myConfig::passwordKey)
                    .applyToComponent {
                        emptyText.text = MiseDatabaseAuthConfig.PROP_PASSWORD_KEY_DEFAULT
                    }
            }.layout(RowLayout.PARENT_GRID)
        }.withMinimumWidth(-1)
            .withMinimumHeight(-1)
            .withPreferredWidth(100)
            .withMaximumSize(-1, -1)
            .apply {
                setMinimumWidthForAllRowLabels(100)
            }

    override fun getComponent(): JComponent = panel

    override fun getPreferredFocusedComponent(): JComponent = panel.preferredFocusedComponent!!

    override fun save(
        config: DatabaseConnectionConfig,
        copyCredentials: Boolean,
    ) {
        panel.apply()
        config.setAdditionalProperty(MiseDatabaseAuthConfig.PROP_USERNAME_KEY, myConfig.usernameKey)
        config.setAdditionalProperty(MiseDatabaseAuthConfig.PROP_PASSWORD_KEY, myConfig.passwordKey)
    }

    override fun reset(
        point: DatabaseConnectionPoint,
        resetCredentials: Boolean,
    ) {
        myConfig.usernameKey = point.getAdditionalProperty(MiseDatabaseAuthConfig.PROP_USERNAME_KEY) ?: ""
        myConfig.passwordKey = point.getAdditionalProperty(MiseDatabaseAuthConfig.PROP_PASSWORD_KEY) ?: ""
        panel.reset()
    }

    override fun onChanged(r: Runnable) {
    }

    override fun forceSave() {
    }

    override fun updateFromUrl(holder: ParametersHolder) {
    }

    override fun updateUrl(model: MutableParametersHolder) {
    }

    override fun hidePassword() {
    }

    override fun isPasswordChanged(): Boolean = false

    override fun reloadCredentials() {
    }
}
