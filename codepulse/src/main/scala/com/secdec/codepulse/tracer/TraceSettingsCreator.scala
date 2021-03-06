/*
 * Code Pulse: A real-time code coverage testing tool. For more information
 * see http://code-pulse.com
 *
 * Copyright (C) 2014 Applied Visions - http://securedecisions.avi.com
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

package com.secdec.codepulse.tracer

import com.codedx.codepulse.hq.config.TraceSettings
import com.secdec.codepulse.data.bytecode.CodeTreeNodeKind
import com.secdec.codepulse.data.jsp.JspMapper
import com.secdec.codepulse.data.model.ProjectData

object TraceSettingsCreator {

	def generateTraceSettings(projectData: ProjectData, jspMapper: Option[JspMapper]): TraceSettings = {
		val treeNodeData = projectData.treeNodeData
		import treeNodeData.ExtendedTreeNodeData

		val inclusions = treeNodeData.iterate { it =>
			(for {
				treeNode <- it
				if treeNode.kind == CodeTreeNodeKind.Pkg
				traced = treeNode.traced getOrElse ???
				if traced
			} yield {
				val slashedName = treeNode.label.replace('.', '/')
				"^" + slashedName + "/[^/]+$"
			}).toList
		} ++ jspMapper.map { mapper =>
			treeNodeData.iterateJspMappings { it =>
				(for ((jspClass, _) <- it) yield mapper getInclusion jspClass).toList
			}
		}.getOrElse(Nil)

		TraceSettings(exclusions = List(".*"), inclusions)
	}
}