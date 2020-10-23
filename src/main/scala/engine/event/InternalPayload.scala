package engine.event

import engine.common.ITuple

case class InternalPayload(payload:Array[ITuple]) extends DataEvent
