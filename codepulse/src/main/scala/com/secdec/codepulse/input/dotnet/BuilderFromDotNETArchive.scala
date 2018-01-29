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

package com.secdec.codepulse.input.dotnet

import java.io.File

import com.secdec.codepulse.data.bytecode.{ CodeForestBuilder, CodeTreeNodeKind }
import com.secdec.codepulse.data.dotnet.{ DotNet, SymbolReaderHTTPServiceConnector }
import com.secdec.codepulse.data.model.{ ProjectData, TreeNodeImporter }
import com.secdec.codepulse.input.BuilderFromArchive
import com.secdec.codepulse.util.ZipEntryChecker
import org.apache.commons.io.FilenameUtils

class BuilderFromDotNETArchive(val file: File, val name: String, val cleanup: () => Unit) extends BuilderFromArchive[CodeForestBuilder] {
	override def process(projectData: ProjectData): CodeForestBuilder = {
		process(file, name, cleanup, projectData)
	}

	override def passesQuickCheck(file: File): Boolean = {
		ZipEntryChecker.findFirstEntry(file) { (_, entry, _) =>
			val extension = FilenameUtils.getExtension(entry.getName)
			!entry.isDirectory && (extension == "exe" || extension == "dll")
		}
	}

	override def process(file: File, name: String, cleanup: => Unit, projectData: ProjectData): CodeForestBuilder = {
		val RootGroupName = "dotNET"
		val tracedGroups = (RootGroupName :: Nil).toSet
		val builder = new CodeForestBuilder
		val methodCorrelationsBuilder = collection.mutable.Map.empty[String, Int]
		val dotNETAssemblyFinder = DotNet.AssemblyPairFromZip(file) _

		ZipEntryChecker.forEachEntry(file) { (filename, entry, contents) =>
			val groupName = if (filename == file.getName) RootGroupName else s"Assemblies/${filename substring file.getName.length + 1}"

			if (!entry.isDirectory) {
				dotNETAssemblyFinder(entry) match {
					case Some((assembly, symbols)) => {
						val methods = new SymbolReaderHTTPServiceConnector(assembly, symbols).Methods
						for {
							(sig, size) <- methods
							treeNode <- Option(builder.getOrAddMethod(groupName, sig, size))
						} methodCorrelationsBuilder += (s"${sig.containingClass}.${sig.name};${sig.modifiers};(${sig.params mkString ","});${sig.returnType}" -> treeNode.id)
					}

					case _ => // nothing
				}
			}
		}

		val treemapNodes = builder.condensePathNodes().result
		val methodCorrelations = methodCorrelationsBuilder.result

		if (treemapNodes.isEmpty) {
			throw new NoSuchElementException("No method data found in analyzed upload file.")
		} else {
			val importer = TreeNodeImporter(projectData.treeNodeData)

			importer ++= treemapNodes.toIterable map {
				case (root, node) =>
					node -> (node.kind match {
						case CodeTreeNodeKind.Grp | CodeTreeNodeKind.Pkg => Some(tracedGroups contains root.name)
						case _ => None
					})
			}

			importer.flush

			projectData.treeNodeData.mapMethodSignatures(methodCorrelations)

			// The `creationDate` for project data detected in this manner
			// should use its default value ('now'), as this is when the data
			// was actually 'created'. The `importDate` should remain blank,
			// since this is not an import of a .pulse file.
			projectData.metadata.creationDate = System.currentTimeMillis
		}

		builder
	}
}
