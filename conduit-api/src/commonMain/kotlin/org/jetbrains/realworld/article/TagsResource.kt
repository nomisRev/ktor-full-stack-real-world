package org.jetbrains.realworld.article

import io.ktor.resources.Resource
import org.jetbrains.realworld.Root

@Resource("/tags")
class TagsResource(val root: Root = Root)