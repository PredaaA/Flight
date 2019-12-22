package me.devoxin.flight.parsers

import me.devoxin.flight.api.Context
import java.util.*

class FloatParser : Parser<Float> {

    override fun parse(ctx: Context, param: String): Optional<Float> {
        val f = param.toFloatOrNull() ?: return Optional.empty()
        return Optional.of(f)
    }

}