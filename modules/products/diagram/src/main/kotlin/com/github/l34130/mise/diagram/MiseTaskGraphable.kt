package com.github.l34130.mise.diagram

import com.github.l34130.mise.core.model.MiseTask
import org.toml.lang.psi.TomlFile

sealed interface MiseTaskGraphable

class MiseTaskGraphableTaskWrapper<T : MiseTask>(
    val task: T,
) : MiseTaskGraphable

class MiseTaskGraphableTomlFile(
    val tomlFile: TomlFile,
) : MiseTaskGraphable

object DefaultMiseTaskGraphable : MiseTaskGraphable
