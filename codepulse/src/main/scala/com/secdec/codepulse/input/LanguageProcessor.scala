/*
 * Code Pulse: A real-time code coverage testing tool. For more information
 * see http://code-pulse.com
 *
 * Copyright (C) 2017 Applied Visions - http://securedecisions.avi.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.secdec.codepulse.input

import java.io.File
import java.util.zip.ZipEntry
import org.apache.commons.io.FilenameUtils

import com.secdec.codepulse.data.model.{ SourceDataAccess, TreeNodeDataAccess }
import com.secdec.codepulse.data.storage.Storage

case class CanProcessFile(file: File)

trait LanguageProcessor {
	def sourceExtensions: List[String]

	final def sourceFiles(entry: ZipEntry): Boolean = {
		val extension = FilenameUtils.getExtension(entry.getName)
		sourceExtensions.contains(extension)
	}

	final def sourceType(typeExtension: String)(entry: ZipEntry): Boolean = {
		val extension = FilenameUtils.getExtension(entry.getName)
		typeExtension == extension
	}

	def canProcess(storage: Storage): Boolean
	def process(storage: Storage, treeNodeData: TreeNodeDataAccess, sourceData: SourceDataAccess): Unit
}
