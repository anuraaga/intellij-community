// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.intellij.build.testFramework

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.TestLoggerFactory
import org.jetbrains.intellij.build.*
import org.jetbrains.intellij.build.impl.logging.BuildMessagesImpl
import kotlin.io.path.Path
import kotlin.io.path.copyTo

fun createBuildContext(homePath: String, productProperties: ProductProperties,
                       buildTools: ProprietaryBuildTools,
                       skipDependencySetup: Boolean = false,
                       communityHomePath: String = "$homePath/community",
                       buildOptionsCustomizer: (BuildOptions) -> Unit = {},
): BuildContext {
  val options = BuildOptions()
  options.isSkipDependencySetup = skipDependencySetup
  options.isIsTestBuild = true
  options.buildStepsToSkip.add(BuildOptions.getTEAMCITY_ARTIFACTS_PUBLICATION())
  options.outputRootPath = FileUtil.createTempDirectory("test-build-${productProperties.baseFileName}", null, true).absolutePath
  if (options.pathToCompiledClassesArchive == null && options.pathToCompiledClassesArchivesMetadata == null && options.isIsInDevelopmentMode) {
    //skip compilation when running tests locally
    options.isUseCompiledClassesFromProjectOutput = true
  }
  buildOptionsCustomizer(options)
  return BuildContext.createContext(communityHomePath, homePath, productProperties, buildTools, options)
}

fun runTestBuild(homePath: String, productProperties: ProductProperties, buildTools: ProprietaryBuildTools,
                 communityHomePath: String = "$homePath/community",
                 buildOptionsCustomizer: (BuildOptions) -> Unit = {},
) {
  val buildContext = createBuildContext(homePath, productProperties, buildTools, false, communityHomePath, buildOptionsCustomizer)
  try {
    BuildTasks.create(buildContext).runTestBuild()
  }
  catch (e: Throwable) {
    val logFile = (buildContext.messages as BuildMessagesImpl).debugLogFile
    val targetFile = Path(TestLoggerFactory.getTestLogDir(), "${productProperties.baseFileName}-test-build-debug.log")
    buildContext.messages.info("Copying debug log to $targetFile")
    logFile.toPath().copyTo(targetFile)
    throw e
  }
}