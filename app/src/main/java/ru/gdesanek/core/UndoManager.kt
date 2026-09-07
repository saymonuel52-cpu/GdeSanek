package ru.gdesanek.core

sealed class Command {
    abstract fun redo()
    abstract fun undo()

    data class InsertObject(val apply: () -> Unit, val revert: () -> Unit) : Command() {
        override fun redo() = apply()
        override fun undo() = revert()
    }
    data class DeleteObject(val apply: () -> Unit, val revert: () -> Unit) : Command() {
        override fun redo() = apply()
        override fun undo() = revert()
    }
    data class UpdateObject(val apply: () -> Unit, val revert: () -> Unit) : Command() {
        override fun redo() = apply()
        override fun undo() = revert()
    }
    data class MoveObject(val apply: () -> Unit, val revert: () -> Unit) : Command() {
        override fun redo() = apply()
        override fun undo() = revert()
    }
}

class UndoManager(private val limit: Int = 50) {
    private val undoStack = ArrayDeque<Command>()
    private val redoStack = ArrayDeque<Command>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun push(cmd: Command) {
        undoStack.addLast(cmd)
        redoStack.clear()
        while (undoStack.size > limit) undoStack.removeFirst()
    }

    fun undo(): Boolean {
        val cmd = undoStack.removeLastOrNull() ?: return false
        cmd.undo()
        redoStack.addLast(cmd)
        return true
    }

    fun redo(): Boolean {
        val cmd = redoStack.removeLastOrNull() ?: return false
        cmd.redo()
        undoStack.addLast(cmd)
        return true
    }

    fun clear() { undoStack.clear(); redoStack.clear() }
}
