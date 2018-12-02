(function (root, factory) {
  if (typeof define === 'function' && define.amd)
    define(['exports', 'kotlin'], factory);
  else if (typeof exports === 'object')
    factory(module.exports, require('kotlin'));
  else {
    if (typeof kotlin === 'undefined') {
      throw new Error("Error loading module 'Q-Gress'. Its dependency 'kotlin' was not found. Please, check whether 'kotlin' is loaded prior to 'Q-Gress'.");
    }
    root['Q-Gress'] = factory(typeof this['Q-Gress'] === 'undefined' ? {} : this['Q-Gress'], kotlin);
  }
}(this, function (_, Kotlin) {
  'use strict';
  var Kind_OBJECT = Kotlin.Kind.OBJECT;
  var Kind_CLASS = Kotlin.Kind.CLASS;
  var listOf = Kotlin.kotlin.collections.listOf_i5x0yv$;
  var ensureNotNull = Kotlin.ensureNotNull;
  var equals = Kotlin.equals;
  var to = Kotlin.kotlin.to_ujzrz7$;
  var toMap = Kotlin.kotlin.collections.toMap_6hr0sd$;
  var emptyMap = Kotlin.kotlin.collections.emptyMap_q3lmfv$;
  var throwCCE = Kotlin.throwCCE;
  var plus = Kotlin.kotlin.collections.plus_mydzjv$;
  var listOf_0 = Kotlin.kotlin.collections.listOf_mh5how$;
  var Unit = Kotlin.kotlin.Unit;
  var take = Kotlin.kotlin.collections.take_ba2ldo$;
  var Kind_INTERFACE = Kotlin.Kind.INTERFACE;
  var toSet = Kotlin.kotlin.collections.toSet_7wnvza$;
  var sum = Kotlin.kotlin.collections.sum_plj8ka$;
  var toList = Kotlin.kotlin.collections.toList_abgq59$;
  var zip = Kotlin.kotlin.collections.zip_45mdf7$;
  var numberToInt = Kotlin.numberToInt;
  var first = Kotlin.kotlin.collections.first_2p1efm$;
  var distinct = Kotlin.kotlin.collections.distinct_7wnvza$;
  var IllegalStateException_init = Kotlin.kotlin.IllegalStateException_init_pdl1vj$;
  var filterNotNull = Kotlin.kotlin.collections.filterNotNull_m3lr2h$;
  var toString = Kotlin.toString;
  var hashCode = Kotlin.hashCode;
  var IntRange = Kotlin.kotlin.ranges.IntRange;
  var getValue = Kotlin.kotlin.collections.getValue_t9ocha$;
  var Enum = Kotlin.kotlin.Enum;
  var throwISE = Kotlin.throwISE;
  var eachCount = Kotlin.kotlin.collections.eachCount_kji7v9$;
  var flatten = Kotlin.kotlin.collections.flatten_u0ad8z$;
  var firstOrNull = Kotlin.kotlin.collections.firstOrNull_2p1efm$;
  var emptyList = Kotlin.kotlin.collections.emptyList_287e2$;
  var mapOf = Kotlin.kotlin.collections.mapOf_qfcya0$;
  var math = Kotlin.kotlin.math;
  var asList = Kotlin.kotlin.collections.asList_us0mfu$;
  var NotImplementedError = Kotlin.kotlin.NotImplementedError;
  var toList_0 = Kotlin.kotlin.collections.toList_7wnvza$;
  var last = Kotlin.kotlin.collections.last_2p1efm$;
  var linkedSetOf = Kotlin.kotlin.collections.linkedSetOf_i5x0yv$;
  var unboxChar = Kotlin.unboxChar;
  var toBoxedChar = Kotlin.toBoxedChar;
  var round = Kotlin.kotlin.math.round_14dthe$;
  var toMutableList = Kotlin.kotlin.collections.toMutableList_4c7yge$;
  var sort = Kotlin.kotlin.collections.sort_4wi501$;
  var mutableListOf = Kotlin.kotlin.collections.mutableListOf_i5x0yv$;
  var toMutableMap = Kotlin.kotlin.collections.toMutableMap_abgq59$;
  var Triple = Kotlin.kotlin.Triple;
  var reversed = Kotlin.kotlin.collections.reversed_7wnvza$;
  var takeLast = Kotlin.kotlin.collections.takeLast_yzln2o$;
  var putAll = Kotlin.kotlin.collections.putAll_cweazw$;
  var shuffled = Kotlin.kotlin.collections.shuffled_7wnvza$;
  var zipWithNext = Kotlin.kotlin.collections.zipWithNext_7wnvza$;
  var until = Kotlin.kotlin.ranges.until_dqglrj$;
  var max = Kotlin.kotlin.collections.max_exjks8$;
  var toByte = Kotlin.toByte;
  var kotlin_js_internal_ByteCompanionObject = Kotlin.kotlin.js.internal.ByteCompanionObject;
  var IllegalArgumentException_init = Kotlin.kotlin.IllegalArgumentException_init_pdl1vj$;
  var first_0 = Kotlin.kotlin.collections.first_7wnvza$;
  var toDoubleOrNull = Kotlin.kotlin.text.toDoubleOrNull_pdl1vz$;
  var addClass = Kotlin.kotlin.dom.addClass_hhb33f$;
  var contains = Kotlin.kotlin.text.contains_li3zpu$;
  var padEnd = Kotlin.kotlin.text.padEnd_vrc1nu$;
  var removeClass = Kotlin.kotlin.dom.removeClass_hhb33f$;
  var split = Kotlin.kotlin.text.split_ip8yn$;
  var replace = Kotlin.kotlin.text.replace_680rmw$;
  var toBoolean = Kotlin.kotlin.text.toBoolean_pdl1vz$;
  var trimMargin = Kotlin.kotlin.text.trimMargin_rjktp$;
  var toMap_0 = Kotlin.kotlin.collections.toMap_abgq59$;
  var count = Kotlin.kotlin.collections.count_7wnvza$;
  var listOfNotNull = Kotlin.kotlin.collections.listOfNotNull_jurz7g$;
  var joinToString = Kotlin.kotlin.collections.joinToString_fmv235$;
  var endsWith = Kotlin.kotlin.text.endsWith_sgbm27$;
  var dropLast = Kotlin.kotlin.text.dropLast_6ic1pp$;
  var padStart = Kotlin.kotlin.text.padStart_vrc1nu$;
  var throwUPAE = Kotlin.throwUPAE;
  var clear = Kotlin.kotlin.dom.clear_asww5s$;
  Faction.prototype = Object.create(Enum.prototype);
  Faction.prototype.constructor = Faction;
  Location.prototype = Object.create(Enum.prototype);
  Location.prototype.constructor = Location;
  VectorStyle.prototype = Object.create(Enum.prototype);
  VectorStyle.prototype.constructor = VectorStyle;
  PortalLevel.prototype = Object.create(Enum.prototype);
  PortalLevel.prototype.constructor = PortalLevel;
  PowerCubeLevel.prototype = Object.create(Enum.prototype);
  PowerCubeLevel.prototype.constructor = PowerCubeLevel;
  ResonatorLevel.prototype = Object.create(Enum.prototype);
  ResonatorLevel.prototype.constructor = ResonatorLevel;
  UltraStrikeLevel.prototype = Object.create(Enum.prototype);
  UltraStrikeLevel.prototype.constructor = UltraStrikeLevel;
  XmpLevel.prototype = Object.create(Enum.prototype);
  XmpLevel.prototype.constructor = XmpLevel;
  LinkAmpType.prototype = Object.create(Enum.prototype);
  LinkAmpType.prototype.constructor = LinkAmpType;
  ModType.prototype = Object.create(Enum.prototype);
  ModType.prototype.constructor = ModType;
  MultihackType.prototype = Object.create(Enum.prototype);
  MultihackType.prototype.constructor = MultihackType;
  ShieldType.prototype = Object.create(Enum.prototype);
  ShieldType.prototype.constructor = ShieldType;
  VirusType.prototype = Object.create(Enum.prototype);
  VirusType.prototype.constructor = VirusType;
  Cooldown.prototype = Object.create(Enum.prototype);
  Cooldown.prototype.constructor = Cooldown;
  ModSlot.prototype = Object.create(Enum.prototype);
  ModSlot.prototype.constructor = ModSlot;
  Octant.prototype = Object.create(Enum.prototype);
  Octant.prototype.constructor = Octant;
  Quality.prototype = Object.create(Enum.prototype);
  Quality.prototype.constructor = Quality;
  Cycle.prototype = Object.create(Enum.prototype);
  Cycle.prototype.constructor = Cycle;
  LoadingText.prototype = Object.create(Loading.prototype);
  LoadingText.prototype.constructor = LoadingText;
  NpcBar.prototype = Object.create(Loading.prototype);
  NpcBar.prototype.constructor = NpcBar;
  VectorBar.prototype = Object.create(Loading.prototype);
  VectorBar.prototype.constructor = VectorBar;
  TopAgentsDisplay.prototype = Object.create(UiTable.prototype);
  TopAgentsDisplay.prototype.constructor = TopAgentsDisplay;
  function Action(item, untilTick) {
    Action$Companion_getInstance();
    this.item = item;
    this.untilTick = untilTick;
  }
  Action.prototype.start_fyi6w8$ = function (item) {
    this.item = item;
    this.untilTick = World_getInstance().tick + Time_getInstance().secondsToTicks_za3lpa$(item.durationSeconds) | 0;
  };
  Action.prototype.end = function () {
    this.item = ActionItem$Companion_getInstance().WAIT;
    this.untilTick = World_getInstance().tick + 1 | 0;
  };
  Action.prototype.toString = function () {
    return this.item.text;
  };
  Action.prototype.isBusy = function () {
    return World_getInstance().tick <= this.untilTick;
  };
  function Action$Companion() {
    Action$Companion_instance = this;
  }
  Action$Companion.prototype.create = function () {
    return new Action(ActionItem$Companion_getInstance().WAIT, World_getInstance().tick);
  };
  Action$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Action$Companion_instance = null;
  function Action$Companion_getInstance() {
    if (Action$Companion_instance === null) {
      new Action$Companion();
    }
    return Action$Companion_instance;
  }
  Action.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Action',
    interfaces: []
  };
  Action.prototype.component1 = function () {
    return this.item;
  };
  Action.prototype.component2 = function () {
    return this.untilTick;
  };
  Action.prototype.copy_34yqkq$ = function (item, untilTick) {
    return new Action(item === void 0 ? this.item : item, untilTick === void 0 ? this.untilTick : untilTick);
  };
  Action.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.item) | 0;
    result = result * 31 + Kotlin.hashCode(this.untilTick) | 0;
    return result;
  };
  Action.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.item, other.item) && Kotlin.equals(this.untilTick, other.untilTick)))));
  };
  function ActionItem(text, durationSeconds, qName) {
    ActionItem$Companion_getInstance();
    this.text = text;
    this.durationSeconds = durationSeconds;
    this.qName = qName;
  }
  var collectionSizeOrDefault = Kotlin.kotlin.collections.collectionSizeOrDefault_ba2ldo$;
  var ArrayList_init = Kotlin.kotlin.collections.ArrayList_init_ww73n8$;
  function ActionItem$Companion() {
    ActionItem$Companion_instance = this;
    this.MOVE = new ActionItem('moving', 60, 'Move');
    this.WAIT = new ActionItem('waiting', 10, 'Wait');
    this.RECHARGE = new ActionItem('recharging', 30, 'Recharge');
    this.RECRUIT = new ActionItem('recruiting', 120, 'Recruit');
    this.EXPLORE = new ActionItem('exploring', 300, 'Explore');
    this.RECYCLE = new ActionItem('recycling', 30, 'Recycle');
    this.HACK = new ActionItem('hacking', 10, 'Hack');
    this.GLYPH = new ActionItem('glyphing', 60, 'Glyph');
    this.ATTACK = new ActionItem('attacking', 15, 'Attack');
    this.DEPLOY = new ActionItem('deploying', 15, 'Deploy');
    this.CAPTURE = new ActionItem('capturing', 15, 'Capture');
    this.LINK = new ActionItem('linking', 30, 'Link');
    var tmp$;
    if (HtmlUtil_getInstance().isRunningInBrowser()) {
      var $receiver = this.values();
      var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
      var tmp$_0;
      tmp$_0 = $receiver.iterator();
      while (tmp$_0.hasNext()) {
        var item = tmp$_0.next();
        destination.add_11rb$(to(item, this.drawTemplate_0(item, Faction$ENL_getInstance())));
      }
      tmp$ = toMap(destination);
    }
     else
      tmp$ = emptyMap();
    this.enlImages_0 = tmp$;
    var tmp$_1;
    if (HtmlUtil_getInstance().isRunningInBrowser()) {
      var $receiver_0 = this.values();
      var destination_0 = ArrayList_init(collectionSizeOrDefault($receiver_0, 10));
      var tmp$_2;
      tmp$_2 = $receiver_0.iterator();
      while (tmp$_2.hasNext()) {
        var item_0 = tmp$_2.next();
        destination_0.add_11rb$(to(item_0, this.drawTemplate_0(item_0, Faction$RES_getInstance())));
      }
      tmp$_1 = toMap(destination_0);
    }
     else
      tmp$_1 = emptyMap();
    this.resImages_0 = tmp$_1;
    var tmp$_3;
    if (HtmlUtil_getInstance().isRunningInBrowser()) {
      var $receiver_1 = this.values();
      var destination_1 = ArrayList_init(collectionSizeOrDefault($receiver_1, 10));
      var tmp$_4;
      tmp$_4 = $receiver_1.iterator();
      while (tmp$_4.hasNext()) {
        var item_1 = tmp$_4.next();
        destination_1.add_11rb$(to(item_1, this.drawTemplate_0(item_1, Faction$NONE_getInstance())));
      }
      tmp$_3 = toMap(destination_1);
    }
     else
      tmp$_3 = emptyMap();
    this.nonImages_0 = tmp$_3;
  }
  ActionItem$Companion.prototype.values = function () {
    return listOf([this.MOVE, this.WAIT, this.RECHARGE, this.RECRUIT, this.EXPLORE, this.RECYCLE, this.HACK, this.GLYPH, this.ATTACK, this.DEPLOY, this.CAPTURE, this.LINK]);
  };
  ActionItem$Companion.prototype.getIcon_5bvev3$ = function (item, faction) {
    if (faction === void 0)
      faction = Faction$NONE_getInstance();
    var tmp$, tmp$_0, tmp$_1, tmp$_2;
    switch (faction.name) {
      case 'ENL':
        tmp$_2 = (tmp$ = this.enlImages_0.get_11rb$(item)) != null ? tmp$ : ensureNotNull(this.enlImages_0.get_11rb$(this.WAIT));
        break;
      case 'RES':
        tmp$_2 = (tmp$_0 = this.resImages_0.get_11rb$(item)) != null ? tmp$_0 : ensureNotNull(this.resImages_0.get_11rb$(this.WAIT));
        break;
      default:tmp$_2 = (tmp$_1 = this.nonImages_0.get_11rb$(item)) != null ? tmp$_1 : ensureNotNull(this.nonImages_0.get_11rb$(this.WAIT));
        break;
    }
    return tmp$_2;
  };
  function ActionItem$Companion$drawTemplate$drawAgentLine(closure$strokeStyle) {
    return function (ctx, line) {
      DrawUtil_getInstance().drawLine_ovbgws$(ctx, line, closure$strokeStyle, 0.7);
    };
  }
  function ActionItem$Companion$drawTemplate$drawAgentCircle(closure$strokeStyle) {
    return function (ctx, circle) {
      DrawUtil_getInstance().drawCircle_3kie0f$(ctx, circle, closure$strokeStyle, 1.0);
    };
  }
  function ActionItem$Companion$drawTemplate$lambda(closure$rr, closure$r, closure$strokeStyle, closure$lw, closure$faction, closure$actionItem, closure$drawAgentCircle, this$ActionItem$, closure$w, closure$h, closure$drawAgentLine) {
    return function (ctx) {
      var tmp$;
      var pos = Coords_init(closure$rr, closure$rr);
      var circle = new Circle(pos, closure$r + 1);
      DrawUtil_getInstance().drawCircle_3kie0f$(ctx, circle, closure$strokeStyle, closure$lw, closure$faction.color);
      tmp$ = closure$actionItem;
      if (equals(tmp$, this$ActionItem$.MOVE))
        closure$drawAgentCircle(ctx, new Circle(pos, closure$rr - 2.0));
      else if (equals(tmp$, this$ActionItem$.EXPLORE)) {
        var off = 2;
        closure$drawAgentLine(ctx, new Line(Coords_init(off, off), Coords_init(closure$w - off | 0, closure$h - off | 0)));
        closure$drawAgentLine(ctx, new Line(Coords_init(off, closure$h - off | 0), Coords_init(closure$w - off | 0, off)));
        closure$drawAgentLine(ctx, new Line(Coords_init(closure$rr, 0), Coords_init(closure$rr, closure$h)));
        closure$drawAgentLine(ctx, new Line(Coords_init(0, closure$rr), Coords_init(closure$w, closure$rr)));
        closure$drawAgentCircle(ctx, new Circle(pos, closure$rr - 2.0));
      }
       else if (equals(tmp$, this$ActionItem$.RECRUIT)) {
        closure$drawAgentLine(ctx, new Line(Coords_init(closure$rr, 0), Coords_init(closure$rr, closure$h)));
        closure$drawAgentLine(ctx, new Line(Coords_init(0, closure$rr), Coords_init(closure$w, closure$rr)));
      }
       else if (equals(tmp$, this$ActionItem$.ATTACK))
        closure$drawAgentLine(ctx, new Line(Coords_init(closure$rr, 0), Coords_init(closure$rr, closure$h)));
      else if (equals(tmp$, this$ActionItem$.LINK))
        closure$drawAgentLine(ctx, new Line(Coords_init(0, closure$rr), Coords_init(closure$w, closure$rr)));
      else if (equals(tmp$, this$ActionItem$.DEPLOY)) {
        closure$drawAgentLine(ctx, new Line(Coords_init(0, closure$rr - 1 | 0), Coords_init(closure$w, closure$rr - 1 | 0)));
        closure$drawAgentLine(ctx, new Line(Coords_init(0, closure$rr + 1 | 0), Coords_init(closure$w, closure$rr + 1 | 0)));
      }
       else if (equals(tmp$, this$ActionItem$.CAPTURE)) {
        closure$drawAgentLine(ctx, new Line(Coords_init(closure$rr, 0), Coords_init(closure$rr, closure$h)));
        closure$drawAgentLine(ctx, new Line(Coords_init(0, closure$rr - 1 | 0), Coords_init(closure$w, closure$rr - 1 | 0)));
        closure$drawAgentLine(ctx, new Line(Coords_init(0, closure$rr + 1 | 0), Coords_init(closure$w, closure$rr + 1 | 0)));
      }
       else if (equals(tmp$, this$ActionItem$.HACK))
        closure$drawAgentCircle(ctx, new Circle(pos, closure$rr - 4.0));
      else if (equals(tmp$, this$ActionItem$.GLYPH))
        closure$drawAgentCircle(ctx, new Circle(pos, closure$rr - 3.0));
      else if (equals(tmp$, this$ActionItem$.RECHARGE)) {
        var off_0 = 2;
        closure$drawAgentLine(ctx, new Line(Coords_init(off_0, closure$h - off_0 | 0), Coords_init(closure$w - off_0 | 0, off_0)));
        closure$drawAgentCircle(ctx, new Circle(pos, closure$rr - 2.0));
      }
       else if (equals(tmp$, this$ActionItem$.RECYCLE)) {
        var off_1 = 2;
        closure$drawAgentLine(ctx, new Line(Coords_init(off_1, off_1), Coords_init(closure$w - off_1 | 0, closure$h - off_1 | 0)));
        closure$drawAgentCircle(ctx, new Circle(pos, closure$rr - 2.0));
      }
       else
        equals(tmp$, this$ActionItem$.WAIT);
    };
  }
  ActionItem$Companion.prototype.drawTemplate_0 = function (actionItem, faction) {
    if (HtmlUtil_getInstance().isNotRunningInBrowser())
      return null;
    var strokeStyle = Colors_getInstance().black;
    var lw = Dim_getInstance().agentLineWidth;
    var r = Dim_getInstance().agentRadius;
    var rr = r + lw | 0;
    var w = rr * 2 | 0;
    var h = w;
    var drawAgentLine = ActionItem$Companion$drawTemplate$drawAgentLine(strokeStyle);
    var drawAgentCircle = ActionItem$Companion$drawTemplate$drawAgentCircle(strokeStyle);
    return HtmlUtil_getInstance().preRender_yb5akz$(w, h, ActionItem$Companion$drawTemplate$lambda(rr, r, strokeStyle, lw, faction, actionItem, drawAgentCircle, this, w, h, drawAgentLine));
  };
  ActionItem$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var ActionItem$Companion_instance = null;
  function ActionItem$Companion_getInstance() {
    if (ActionItem$Companion_instance === null) {
      new ActionItem$Companion();
    }
    return ActionItem$Companion_instance;
  }
  ActionItem.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ActionItem',
    interfaces: []
  };
  ActionItem.prototype.component1 = function () {
    return this.text;
  };
  ActionItem.prototype.component2 = function () {
    return this.durationSeconds;
  };
  ActionItem.prototype.component3 = function () {
    return this.qName;
  };
  ActionItem.prototype.copy_h6sd2a$ = function (text, durationSeconds, qName) {
    return new ActionItem(text === void 0 ? this.text : text, durationSeconds === void 0 ? this.durationSeconds : durationSeconds, qName === void 0 ? this.qName : qName);
  };
  ActionItem.prototype.toString = function () {
    return 'ActionItem(text=' + Kotlin.toString(this.text) + (', durationSeconds=' + Kotlin.toString(this.durationSeconds)) + (', qName=' + Kotlin.toString(this.qName)) + ')';
  };
  ActionItem.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.text) | 0;
    result = result * 31 + Kotlin.hashCode(this.durationSeconds) | 0;
    result = result * 31 + Kotlin.hashCode(this.qName) | 0;
    return result;
  };
  ActionItem.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.text, other.text) && Kotlin.equals(this.durationSeconds, other.durationSeconds) && Kotlin.equals(this.qName, other.qName)))));
  };
  function ActionSelector() {
    ActionSelector_instance = this;
  }
  ActionSelector.prototype.doSomethingElse_912u9o$ = function (agent) {
    var tmp$, tmp$_0, tmp$_1;
    var portalFaction = (tmp$_0 = (tmp$ = agent.actionPortal.owner) != null ? tmp$.faction : null) != null ? tmp$_0 : Faction$NONE_getInstance();
    if (!agent.isAtActionPortal())
      tmp$_1 = this.doAnywhereAction_0(agent);
    else if (portalFaction === Faction$NONE_getInstance())
      tmp$_1 = this.doNeutralPortalAction_0(agent);
    else if (portalFaction === agent.faction)
      tmp$_1 = this.doFriendlyPortalAction_0(agent);
    else
      tmp$_1 = this.doEnemyPortalAction_0(agent);
    return tmp$_1;
  };
  ActionSelector.prototype.q_aafct0$ = function (faction, value) {
    var tmp$;
    var id = value.id + 'Slider' + faction.nickName;
    var slider = Kotlin.isType(tmp$ = window.document.getElementById(id), HTMLInputElement) ? tmp$ : throwCCE();
    return slider.valueAsNumber * value.weight;
  };
  function ActionSelector$default$lambda(closure$agent) {
    return function () {
      return closure$agent.doNothing();
    };
  }
  ActionSelector.prototype.default_0 = function (agent) {
    return ActionSelector$default$lambda(agent);
  };
  ActionSelector.prototype.doAnywhereAction_0 = function (agent) {
    return Util_getInstance().select_4u7aq8$(this.actionsForAnywhere_0(agent), this.default_0(agent))();
  };
  ActionSelector.prototype.doNeutralPortalAction_0 = function (agent) {
    return Util_getInstance().select_4u7aq8$(this.actionsForNeutralPortals_0(agent), this.default_0(agent))();
  };
  ActionSelector.prototype.doFriendlyPortalAction_0 = function (agent) {
    return Util_getInstance().select_4u7aq8$(this.actionsForFriendlyPortals_0(agent), this.default_0(agent))();
  };
  ActionSelector.prototype.doEnemyPortalAction_0 = function (agent) {
    return Util_getInstance().select_4u7aq8$(this.actionsForEnemyPortals_0(agent), this.default_0(agent))();
  };
  function ActionSelector$actionsForAnywhere$lambda(closure$agent) {
    return function () {
      return closure$agent.moveElsewhere();
    };
  }
  function ActionSelector$actionsForAnywhere$lambda_0(closure$agent) {
    return function () {
      return Recycler_getInstance().performAction_912u9o$(closure$agent);
    };
  }
  function ActionSelector$actionsForAnywhere$lambda_1(closure$agent) {
    return function () {
      return Recharger_getInstance().performAction_912u9o$(closure$agent);
    };
  }
  function ActionSelector$actionsForAnywhere$lambda_2(closure$agent) {
    return function () {
      return Recruiter_getInstance().performAction_912u9o$(closure$agent);
    };
  }
  function ActionSelector$actionsForAnywhere$lambda_3(closure$agent) {
    return function () {
      return Explorer_getInstance().performAction_912u9o$(closure$agent);
    };
  }
  ActionSelector.prototype.actionsForAnywhere_0 = function (agent) {
    var moveElsewhereQ = this.q_aafct0$(agent.faction, QActions_getInstance().MOVE_ELSEWHERE);
    var recycleQ = Recycler_getInstance().isActionPossible_912u9o$(agent) ? this.q_aafct0$(agent.faction, QActions_getInstance().RECYCLE) : -1.0;
    var rechargeQ = Recharger_getInstance().isActionPossible_912u9o$(agent) ? this.q_aafct0$(agent.faction, QActions_getInstance().RECHARGE) : -1.0;
    var recruitQ = Recruiter_getInstance().isActionPossible_912u9o$(agent) ? this.q_aafct0$(agent.faction, QActions_getInstance().RECRUIT) : -1.0;
    var exploreQ = Explorer_getInstance().isActionPossible_912u9o$(agent) ? this.q_aafct0$(agent.faction, QActions_getInstance().EXPLORE) : -1.0;
    return listOf([to(moveElsewhereQ, ActionSelector$actionsForAnywhere$lambda(agent)), to(recycleQ, ActionSelector$actionsForAnywhere$lambda_0(agent)), to(rechargeQ, ActionSelector$actionsForAnywhere$lambda_1(agent)), to(recruitQ, ActionSelector$actionsForAnywhere$lambda_2(agent)), to(exploreQ, ActionSelector$actionsForAnywhere$lambda_3(agent))]);
  };
  function ActionSelector$actionsForPortals$lambda(closure$agent) {
    return function () {
      return Hacker_getInstance().performAction_912u9o$(closure$agent);
    };
  }
  function ActionSelector$actionsForPortals$lambda_0(closure$agent) {
    return function () {
      return Glypher_getInstance().performAction_912u9o$(closure$agent);
    };
  }
  ActionSelector.prototype.actionsForPortals_0 = function (agent) {
    var basicValues = this.actionsForAnywhere_0(agent);
    var hackQ = Hacker_getInstance().isActionPossible_912u9o$(agent) ? this.q_aafct0$(agent.faction, QActions_getInstance().HACK) : -1.0;
    var glyphQ = Glypher_getInstance().isActionPossible_912u9o$(agent) ? this.q_aafct0$(agent.faction, QActions_getInstance().GLYPH) : -1.0;
    return plus(basicValues, listOf([to(hackQ, ActionSelector$actionsForPortals$lambda(agent)), to(glyphQ, ActionSelector$actionsForPortals$lambda_0(agent))]));
  };
  function ActionSelector$actionsForNeutralPortals$lambda(closure$agent) {
    return function () {
      return closure$agent.capturePortal_6taknv$(true);
    };
  }
  ActionSelector.prototype.actionsForNeutralPortals_0 = function (agent) {
    var basicValues = this.actionsForPortals_0(agent);
    var captureQ = Deployer_getInstance().isActionPossible_912u9o$(agent) ? this.q_aafct0$(agent.faction, QActions_getInstance().CAPTURE) : -1.0;
    return plus(basicValues, listOf_0(to(captureQ, ActionSelector$actionsForNeutralPortals$lambda(agent))));
  };
  function ActionSelector$actionsForFriendlyPortals$lambda(closure$agent) {
    return function () {
      return closure$agent.deployPortal_6taknv$(true);
    };
  }
  function ActionSelector$actionsForFriendlyPortals$lambda_0(closure$agent) {
    return function () {
      return Linker_getInstance().performAction_912u9o$(closure$agent);
    };
  }
  ActionSelector.prototype.actionsForFriendlyPortals_0 = function (agent) {
    var basicValues = this.actionsForPortals_0(agent);
    var deployQ = Deployer_getInstance().isActionPossible_912u9o$(agent) ? this.q_aafct0$(agent.faction, QActions_getInstance().DEPLOY) : -1.0;
    var linkQ = Linker_getInstance().isActionPossible_912u9o$(agent) ? this.q_aafct0$(agent.faction, QActions_getInstance().LINK) : -1.0;
    return plus(basicValues, listOf([to(deployQ, ActionSelector$actionsForFriendlyPortals$lambda(agent)), to(linkQ, ActionSelector$actionsForFriendlyPortals$lambda_0(agent))]));
  };
  function ActionSelector$actionsForEnemyPortals$lambda(closure$agent) {
    return function () {
      return closure$agent.attackPortal_6taknv$(true);
    };
  }
  ActionSelector.prototype.actionsForEnemyPortals_0 = function (agent) {
    var basicValues = this.actionsForPortals_0(agent);
    var attackQ = Attacker_getInstance().isActionPossible_912u9o$(agent) ? this.q_aafct0$(agent.faction, QActions_getInstance().ATTACK) : -1.0;
    return plus(basicValues, listOf_0(to(attackQ, ActionSelector$actionsForEnemyPortals$lambda(agent))));
  };
  ActionSelector.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'ActionSelector',
    interfaces: []
  };
  var ActionSelector_instance = null;
  function ActionSelector_getInstance() {
    if (ActionSelector_instance === null) {
      new ActionSelector();
    }
    return ActionSelector_instance;
  }
  function Attacker() {
    Attacker_instance = this;
    this.actionItem_332705$_0 = ActionItem$Companion_getInstance().ATTACK;
    this.minAttackXmps_0 = 10;
    this.maxAttackXmps_0 = 16;
    this.attackXmps_0 = 50;
  }
  Object.defineProperty(Attacker.prototype, 'actionItem', {
    get: function () {
      return this.actionItem_332705$_0;
    }
  });
  Attacker.prototype.isActionPossible_912u9o$ = function (agent) {
    return agent.inventory.findXmps().size >= 50;
  };
  Attacker.prototype.performAction_912u9o$ = function (agent) {
    return this.performAction_0(agent, 1);
  };
  Attacker.prototype.performAction_0 = function (agent, i) {
    var xmps = this.xmpsForAttack_0(agent.inventory);
    this.doAttack_0(agent, xmps);
    agent.inventory.consumeXmps_ss5kb$(xmps);
    Queues_getInstance().registerAttack_x4gnsd$(agent, xmps, i);
    var isDoItAgain = !xmps.isEmpty() && Util_getInstance().random() <= 1 / Constants_getInstance().phi;
    return isDoItAgain ? this.performAction_0(agent, i + 1 | 0) : agent;
  };
  Attacker.prototype.doAttack_0 = function (agent, xmps) {
    var tmp$;
    tmp$ = xmps.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      switch (element.level.level) {
        case 1:
          agent.removeXm_za3lpa$(10);
          break;
        case 2:
          agent.removeXm_za3lpa$(20);
          break;
        case 3:
          agent.removeXm_za3lpa$(70);
          break;
        case 4:
          agent.removeXm_za3lpa$(140);
          break;
        case 5:
          agent.removeXm_za3lpa$(250);
          break;
        case 6:
          agent.removeXm_za3lpa$(360);
          break;
        case 7:
          agent.removeXm_za3lpa$(490);
          break;
        default:agent.removeXm_za3lpa$(640);
          break;
      }
    }
  };
  Attacker.prototype.attackXmpCount_0 = function () {
    return 10 + Util_getInstance().randomInt_za3lpa$(6) | 0;
  };
  function Attacker$xmpsForAttack$lambda(it) {
    return it.level;
  }
  var sortedWith = Kotlin.kotlin.collections.sortedWith_eknfly$;
  var wrapFunction = Kotlin.wrapFunction;
  var compareByDescending$lambda = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(b), selector(a));
      };
    };
  });
  var Comparator = Kotlin.kotlin.Comparator;
  function Comparator$ObjectLiteral(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Attacker.prototype.xmpsForAttack_0 = function (inv) {
    return take(sortedWith(inv.findXmps(), new Comparator$ObjectLiteral(compareByDescending$lambda(Attacker$xmpsForAttack$lambda))), this.attackXmpCount_0());
  };
  Attacker.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Attacker',
    interfaces: [ConditionalAction]
  };
  var Attacker_instance = null;
  function Attacker_getInstance() {
    if (Attacker_instance === null) {
      new Attacker();
    }
    return Attacker_instance;
  }
  function ConditionalAction() {
  }
  ConditionalAction.$metadata$ = {
    kind: Kind_INTERFACE,
    simpleName: 'ConditionalAction',
    interfaces: []
  };
  function Deployer() {
    Deployer_instance = this;
    this.actionItem_t5xkto$_0 = ActionItem$Companion_getInstance().DEPLOY;
  }
  Object.defineProperty(Deployer.prototype, 'actionItem', {
    get: function () {
      return this.actionItem_t5xkto$_0;
    }
  });
  var Collection = Kotlin.kotlin.collections.Collection;
  Deployer.prototype.isActionPossible_912u9o$ = function (agent) {
    if (!this.isActionPortalFriendly_0(agent)) {
      return false;
    }
    if (!this.areMoreResosAllowed_0(agent)) {
      return false;
    }
    var inventoryResos = this.inventoryResos_0(agent.inventory);
    if (inventoryResos.isEmpty()) {
      return false;
    }
    var ownedInPortal = this.ownedInPortal_0(agent);
    var $receiver = toSet(inventoryResos);
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(this.maybeDeployReso_0(inventoryResos, ownedInPortal, item, true, agent));
    }
    var results = destination;
    var any$result;
    any$break: do {
      var tmp$_0;
      if (Kotlin.isType(results, Collection) && results.isEmpty()) {
        any$result = false;
        break any$break;
      }
      tmp$_0 = results.iterator();
      while (tmp$_0.hasNext()) {
        var element = tmp$_0.next();
        if (element) {
          any$result = true;
          break any$break;
        }
      }
      any$result = false;
    }
     while (false);
    return any$result;
  };
  Deployer.prototype.performAction_912u9o$ = function (agent) {
    var inventoryResos = this.inventoryResos_0(agent.inventory);
    var ownedInPortal = this.ownedInPortal_0(agent);
    var $receiver = toSet(inventoryResos);
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(this.maybeDeployReso_0(inventoryResos, ownedInPortal, item, false, agent));
    }
    var results = destination;
    var none$result;
    none$break: do {
      var tmp$_0;
      if (Kotlin.isType(results, Collection) && results.isEmpty()) {
        none$result = true;
        break none$break;
      }
      tmp$_0 = results.iterator();
      while (tmp$_0.hasNext()) {
        var element = tmp$_0.next();
        if (element) {
          none$result = false;
          break none$break;
        }
      }
      none$result = true;
    }
     while (false);
    if (none$result) {
      console.warn('Deployment failed..');
    }
    return agent;
  };
  Deployer.prototype.isActionPortalFriendly_0 = function (agent) {
    return !agent.actionPortal.isEnemyOf_912u9o$(agent);
  };
  Deployer.prototype.areMoreResosAllowed_0 = function (agent) {
    var $receiver = this.allowedResoLevels_0(agent);
    var destination = ArrayList_init($receiver.size);
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(item.value);
    }
    return sum(destination) > 0;
  };
  Deployer.prototype.allowedResoLevels_0 = function (agent) {
    return agent.actionPortal.findAllowedResoLevels_912u9o$(agent);
  };
  var LinkedHashMap_init = Kotlin.kotlin.collections.LinkedHashMap_init_q3lmfv$;
  Deployer.prototype.ownedInPortal_0 = function (agent) {
    var $receiver = agent.actionPortal.resoSlots;
    var destination = LinkedHashMap_init();
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.value.isOwnedBy_912u9o$(agent)) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    return toList(destination);
  };
  function Deployer$inventoryResos$lambda(it) {
    return it.level;
  }
  var ArrayList_init_0 = Kotlin.kotlin.collections.ArrayList_init_287e2$;
  var compareByDescending$lambda_0 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(b), selector(a));
      };
    };
  });
  function Comparator$ObjectLiteral_0(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_0.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_0.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Deployer.prototype.inventoryResos_0 = function (inv) {
    var $receiver = inv.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, Resonator))
        destination.add_11rb$(element);
    }
    var destination_0 = ArrayList_init(collectionSizeOrDefault(destination, 10));
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      var tmp$_1;
      destination_0.add_11rb$(Kotlin.isType(tmp$_1 = item, Resonator) ? tmp$_1 : throwCCE());
    }
    return sortedWith(destination_0, new Comparator$ObjectLiteral_0(compareByDescending$lambda_0(Deployer$inventoryResos$lambda)));
  };
  var Math_0 = Math;
  Deployer.prototype.maxDeployable_0 = function (ownedInPortal, reso) {
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = ownedInPortal.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var tmp$_0, tmp$_1, tmp$_2;
      if (((tmp$_2 = (tmp$_1 = (tmp$_0 = element.second.resonator) != null ? tmp$_0.level : null) != null ? tmp$_1.level : null) != null ? tmp$_2 : 0) >= reso.level.level)
        destination.add_11rb$(element);
    }
    var owned = destination.size;
    var a = reso.level.deployablePerPlayer - owned | 0;
    return Math_0.max(a, 0);
  };
  Deployer.prototype.deployableSlots_0 = function (portal, reso) {
    var $receiver = portal.resoSlots;
    var destination = LinkedHashMap_init();
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var tmp$_0, tmp$_1, tmp$_2;
      if (element.value.isEmpty() || ((tmp$_2 = (tmp$_1 = (tmp$_0 = element.value.resonator) != null ? tmp$_0.level : null) != null ? tmp$_1.level : null) != null ? tmp$_2 : 0) < reso.level.level) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    return toList(destination);
  };
  Deployer.prototype.levelResos_0 = function (inventoryResos, reso, agent) {
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = inventoryResos.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.level === reso.level && element.level.level <= agent.getLevel())
        destination.add_11rb$(element);
    }
    return destination;
  };
  Deployer.prototype.deployResos_0 = function (levelResos, maxDeployable) {
    return take(levelResos, maxDeployable);
  };
  Deployer.prototype.actuallyDeploy_0 = function (agent, slots, resos) {
    var portal = agent.actionPortal;
    var $receiver = zip(Util_getInstance().shuffle_bemo1h$(slots), resos);
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(to(item.first.first, item.second));
    }
    var deployMap = toMap(destination);
    var a = agent.distanceToPortal_hv9zn6$(portal);
    var b = Dim_getInstance().minDeploymentRange;
    var distance = Math_0.max(a, b);
    portal.deploy_en6fu0$(agent, deployMap, numberToInt(distance));
    SoundUtil_getInstance().playDeploySound_s1df0o$(portal.location, numberToInt(distance));
    agent.action.start_fyi6w8$(ActionItem$Companion_getInstance().DEPLOY);
  };
  Deployer.prototype.maybeDeployReso_0 = function (inventoryResos, ownedInPortal, reso, isTryOnly, agent) {
    var maxDeployable = this.maxDeployable_0(ownedInPortal, reso);
    if (maxDeployable <= 0) {
      return false;
    }
    var levelResos = this.levelResos_0(inventoryResos, reso, agent);
    if (levelResos == null || levelResos.isEmpty()) {
      return false;
    }
    var resos = this.deployResos_0(levelResos, maxDeployable);
    if (resos == null || resos.isEmpty()) {
      return false;
    }
    var deployableSlots = this.deployableSlots_0(agent.actionPortal, reso);
    if (deployableSlots == null || deployableSlots.isEmpty()) {
      return false;
    }
    if (!isTryOnly) {
      this.actuallyDeploy_0(agent, deployableSlots, resos);
    }
    return true;
  };
  Deployer.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Deployer',
    interfaces: [ConditionalAction]
  };
  var Deployer_instance = null;
  function Deployer_getInstance() {
    if (Deployer_instance === null) {
      new Deployer();
    }
    return Deployer_instance;
  }
  function Explorer() {
    Explorer_instance = this;
    this.actionItem_lesim7$_0 = ActionItem$Companion_getInstance().EXPLORE;
    this.portalDiscoveryChance_0 = 0.2;
  }
  Object.defineProperty(Explorer.prototype, 'actionItem', {
    get: function () {
      return this.actionItem_lesim7$_0;
    }
  });
  Explorer.prototype.isActionPossible_912u9o$ = function (agent) {
    return World_getInstance().countPortals() < 89;
  };
  Explorer.prototype.performAction_912u9o$ = function (agent) {
    agent.action.start_fyi6w8$(this.actionItem);
    if (Util_getInstance().random() <= this.portalDiscoveryChance_0) {
      var newPortal = Portal$Companion_getInstance().createRandom();
      VectorFields_getInstance().draw_hv9zn6$(newPortal);
      World_getInstance().allPortals.add_11rb$(newPortal);
      agent.destination = newPortal.location;
      Com_getInstance().addMessage_61zpoe$(agent.toString() + ' discovered a new portal ' + newPortal + '.');
    }
     else {
      agent.destination = Coords$Companion_getInstance().createRandomForPortal();
    }
    return agent;
  };
  Explorer.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Explorer',
    interfaces: [ConditionalAction]
  };
  var Explorer_instance = null;
  function Explorer_getInstance() {
    if (Explorer_instance === null) {
      new Explorer();
    }
    return Explorer_instance;
  }
  function Glypher() {
    Glypher_instance = this;
    this.actionItem_3ahtxd$_0 = ActionItem$Companion_getInstance().GLYPH;
  }
  Object.defineProperty(Glypher.prototype, 'actionItem', {
    get: function () {
      return this.actionItem_3ahtxd$_0;
    }
  });
  Glypher.prototype.isActionPossible_912u9o$ = function (agent) {
    return Hacker_getInstance().isActionPossible_912u9o$(agent);
  };
  Glypher.prototype.performAction_912u9o$ = function (agent) {
    agent.action.start_fyi6w8$(this.actionItem);
    var glyphResult = agent.actionPortal.tryGlyph_912u9o$(agent);
    SoundUtil_getInstance().playGlyphingSound_lfj9be$(agent.actionPortal.location);
    var isSuccess = glyphResult.items != null;
    if (isSuccess) {
      var newStuff = ensureNotNull(glyphResult.items);
      agent.inventory.items.addAll_brywnq$(newStuff);
    }
    return agent;
  };
  Glypher.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Glypher',
    interfaces: [ConditionalAction]
  };
  var Glypher_instance = null;
  function Glypher_getInstance() {
    if (Glypher_instance === null) {
      new Glypher();
    }
    return Glypher_instance;
  }
  function Hacker() {
    Hacker_instance = this;
    this.actionItem_st3xvy$_0 = ActionItem$Companion_getInstance().HACK;
  }
  Object.defineProperty(Hacker.prototype, 'actionItem', {
    get: function () {
      return this.actionItem_st3xvy$_0;
    }
  });
  Hacker.prototype.isActionPossible_912u9o$ = function (agent) {
    return agent.actionPortal.canHack_912u9o$(agent);
  };
  Hacker.prototype.performAction_912u9o$ = function (agent) {
    agent.action.start_fyi6w8$(this.actionItem);
    var hackResult = agent.actionPortal.tryHack_912u9o$(agent);
    SoundUtil_getInstance().playHackingSound_lfj9be$(agent.actionPortal.location);
    var isSuccess = hackResult.items != null;
    if (isSuccess) {
      var newStuff = ensureNotNull(hackResult.items);
      agent.inventory.items.addAll_brywnq$(newStuff);
    }
    return agent;
  };
  Hacker.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Hacker',
    interfaces: [ConditionalAction]
  };
  var Hacker_instance = null;
  function Hacker_getInstance() {
    if (Hacker_instance === null) {
      new Hacker();
    }
    return Hacker_instance;
  }
  function Linker() {
    Linker_instance = this;
    this.actionItem_m09py1$_0 = ActionItem$Companion_getInstance().LINK;
  }
  Object.defineProperty(Linker.prototype, 'actionItem', {
    get: function () {
      return this.actionItem_m09py1$_0;
    }
  });
  Linker.prototype.isActionPossible_912u9o$ = function (agent) {
    var canLinkOut = agent.actionPortal.canLinkOut_912u9o$(agent);
    var hasKeys = this.hasFriendlyKeys_0(agent);
    var hasTargets = !this.targetOptions_0(agent).isEmpty();
    return canLinkOut && hasKeys && hasTargets;
  };
  Linker.prototype.performAction_912u9o$ = function (agent) {
    agent.action.start_fyi6w8$(this.actionItem);
    var linkOptions = this.targetOptions_0(agent);
    var linkTarget = first(Util_getInstance().shuffle_bemo1h$(linkOptions));
    agent.actionPortal.createLink_g4r5ni$(agent, linkTarget);
    return agent;
  };
  Linker.prototype.hasFriendlyKeys_0 = function (agent) {
    var $receiver = agent.keySet();
    var any$result;
    any$break: do {
      var tmp$;
      if (Kotlin.isType($receiver, Collection) && $receiver.isEmpty()) {
        any$result = false;
        break any$break;
      }
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        if (element.isFriendlyToOwner()) {
          any$result = true;
          break any$break;
        }
      }
      any$result = false;
    }
     while (false);
    return any$result;
  };
  Linker.prototype.linkable_0 = function (agent) {
    return distinct(agent.actionPortal.findLinkableForKeys_912u9o$(agent));
  };
  Linker.prototype.hasNoCrossLinks_0 = function (newline) {
    var $receiver = World_getInstance().allLines();
    var none$result;
    none$break: do {
      var tmp$;
      if (Kotlin.isType($receiver, Collection) && $receiver.isEmpty()) {
        none$result = true;
        break none$break;
      }
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        if (element.doesIntersect_589y3w$(newline)) {
          none$result = false;
          break none$break;
        }
      }
      none$result = true;
    }
     while (false);
    return none$result;
  };
  Linker.prototype.targetOptions_0 = function (agent) {
    var $receiver = this.linkable_0(agent);
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (!(element != null ? element.equals(agent.actionPortal) : null) && element.owner != null && !element.isDeprecated())
        destination.add_11rb$(element);
    }
    var destination_0 = ArrayList_init_0();
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      var tmp$_1;
      var linkLine = (tmp$_1 = Link$Companion_getInstance().create_6ezwqo$(agent.actionPortal, element_0, agent)) != null ? tmp$_1.getLine() : null;
      if (linkLine != null && this.hasNoCrossLinks_0(linkLine))
        destination_0.add_11rb$(element_0);
    }
    return destination_0;
  };
  Linker.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Linker',
    interfaces: [ConditionalAction]
  };
  var Linker_instance = null;
  function Linker_getInstance() {
    if (Linker_instance === null) {
      new Linker();
    }
    return Linker_instance;
  }
  function Recharger() {
    Recharger_instance = this;
    this.actionItem_619hv3$_0 = ActionItem$Companion_getInstance().RECYCLE;
  }
  Object.defineProperty(Recharger.prototype, 'actionItem', {
    get: function () {
      return this.actionItem_619hv3$_0;
    }
  });
  Recharger.prototype.isActionPossible_912u9o$ = function (agent) {
    var tmp$ = agent.isXmFilled();
    if (tmp$) {
      tmp$ = !this.chargeableKeys_0(agent).isEmpty();
    }
    var tmp$_0 = tmp$;
    if (tmp$_0) {
      tmp$_0 = !this.rechargeResos_0(agent).isEmpty();
    }
    return tmp$_0;
  };
  Recharger.prototype.performAction_912u9o$ = function (agent) {
    agent.action.start_fyi6w8$(this.actionItem);
    var resos = this.rechargeResos_0(agent);
    var tmp$;
    tmp$ = resos.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      element.recharge_2b7tta$(agent, 1000 / resos.size | 0);
    }
    return agent;
  };
  Recharger.prototype.chargeableKeys_0 = function (agent) {
    var tmp$ = Portal$Companion_getInstance();
    var $receiver = agent.keySet();
    var $receiver_0 = tmp$.findChargeableForKeys_p3u7jq$(agent, $receiver != null ? $receiver : emptyList());
    return $receiver_0 != null ? $receiver_0 : emptyList();
  };
  function Recharger$lowestChargeablePortal$lambda(it) {
    return it.calcHealth();
  }
  var compareBy$lambda = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_1(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_1.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_1.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Recharger.prototype.lowestChargeablePortal_0 = function (agent) {
    return first(sortedWith(this.chargeableKeys_0(agent), new Comparator$ObjectLiteral_1(compareBy$lambda(Recharger$lowestChargeablePortal$lambda))));
  };
  var mapNotNullTo$lambda = wrapFunction(function () {
    return function (closure$transform, closure$destination) {
      return function (element) {
        var tmp$;
        if ((tmp$ = closure$transform(element)) != null) {
          closure$destination.add_11rb$(tmp$);
        }
        return Unit;
      };
    };
  });
  Recharger.prototype.rechargeResos_0 = function (agent) {
    var $receiver = this.lowestChargeablePortal_0(agent).resoSlots;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var tmp$_0;
      if ((tmp$_0 = element.value.resonator) != null) {
        destination.add_11rb$(tmp$_0);
      }
    }
    return destination;
  };
  Recharger.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Recharger',
    interfaces: [ConditionalAction]
  };
  var Recharger_instance = null;
  function Recharger_getInstance() {
    if (Recharger_instance === null) {
      new Recharger();
    }
    return Recharger_instance;
  }
  function Recruiter() {
    Recruiter_instance = this;
    this.actionItem_xib8j3$_0 = ActionItem$Companion_getInstance().RECRUIT;
  }
  Object.defineProperty(Recruiter.prototype, 'actionItem', {
    get: function () {
      return this.actionItem_xib8j3$_0;
    }
  });
  Recruiter.prototype.isActionPossible_912u9o$ = function (agent) {
    return World_getInstance().canRecruitMore_bip15f$(agent.faction);
  };
  Recruiter.prototype.performAction_912u9o$ = function (agent) {
    var tmp$;
    agent.action.start_fyi6w8$(this.actionItem);
    var npc = NonFaction$Companion_getInstance().findNearestTo_lfj9be$(agent.pos);
    if (Util_getInstance().random() < NonFaction$Companion_getInstance().changeToBeRecruited) {
      World_getInstance().allNonFaction.remove_11rb$(npc);
      switch (agent.faction.name) {
        case 'ENL':
          tmp$ = Agent$Companion_getInstance().createFrog_5edep5$(World_getInstance().grid);
          break;
        case 'RES':
          tmp$ = Agent$Companion_getInstance().createSmurf_5edep5$(World_getInstance().grid);
          break;
        default:throw IllegalStateException_init(agent.toString() + ' is ' + agent.faction + ' NPC.');
      }
      var newAgent = tmp$;
      Com_getInstance().addMessage_61zpoe$(newAgent.toString() + ' has completed the tutorial.');
      World_getInstance().allAgents.add_11rb$(newAgent);
    }
    agent.destination = NonFaction$Companion_getInstance().findNearestTo_lfj9be$(agent.pos).pos;
    return agent;
  };
  Recruiter.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Recruiter',
    interfaces: [ConditionalAction]
  };
  var Recruiter_instance = null;
  function Recruiter_getInstance() {
    if (Recruiter_instance === null) {
      new Recruiter();
    }
    return Recruiter_instance;
  }
  function Recycler() {
    Recycler_instance = this;
    this.actionItem_gn3c5b$_0 = ActionItem$Companion_getInstance().RECYCLE;
  }
  Object.defineProperty(Recycler.prototype, 'actionItem', {
    get: function () {
      return this.actionItem_gn3c5b$_0;
    }
  });
  Recycler.prototype.isActionPossible_912u9o$ = function (agent) {
    return agent.xm < (agent.xmCapacity() / 10 | 0);
  };
  Recycler.prototype.performAction_912u9o$ = function (agent) {
    agent.action.start_fyi6w8$(this.actionItem);
    var cubes = agent.inventory.findPowerCubes();
    if (!cubes.isEmpty()) {
      var cube = first(cubes);
      agent.addXm_za3lpa$(cube.level.calculateRecycleXm());
      agent.inventory.consumeCubes_lz36jp$(listOf_0(cube));
    }
    return agent;
  };
  Recycler.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Recycler',
    interfaces: [ConditionalAction]
  };
  var Recycler_instance = null;
  function Recycler_getInstance() {
    if (Recycler_instance === null) {
      new Recycler();
    }
    return Recycler_instance;
  }
  function Agent(faction, name, pos, skills, inventory, action, actionPortal, destination, lastPosition, ap, xm, velocity) {
    Agent$Companion_getInstance();
    if (ap === void 0)
      ap = 0;
    if (xm === void 0)
      xm = 0;
    if (velocity === void 0)
      velocity = Complex$Companion_getInstance().ZERO;
    this.faction = faction;
    this.name = name;
    this.pos = pos;
    this.skills = skills;
    this.inventory = inventory;
    this.action = action;
    this.actionPortal = actionPortal;
    this.destination = destination;
    this.lastPosition_0 = lastPosition;
    this.ap = ap;
    this.xm = xm;
    this.velocity = velocity;
  }
  Agent.prototype.key = function () {
    return this.toString();
  };
  Agent.prototype.distanceToDestination_0 = function () {
    return this.pos.distanceTo_lfj9be$(this.destination);
  };
  Agent.prototype.distanceToPortal_hv9zn6$ = function (portal) {
    return this.pos.distanceTo_lfj9be$(portal.location);
  };
  Agent.prototype.isAtActionPortal = function () {
    return this.distanceToPortal_hv9zn6$(this.actionPortal) < Dim_getInstance().maxDeploymentRange;
  };
  Agent.prototype.lineToPortal_0 = function (portal) {
    return new Line(this.pos, portal.location);
  };
  Agent.prototype.lineToDestination_0 = function () {
    return new Line(this.pos, this.destination);
  };
  Agent.prototype.getLevel = function () {
    return Agent$Companion_getInstance().getLevel_0(this.ap);
  };
  Agent.prototype.xmCapacity = function () {
    return Agent$Companion_getInstance().xmCapacity_0(this.getLevel());
  };
  Agent.prototype.calcAbsXmBar_0 = function () {
    var tmp$ = this.xmCapacity();
    var b = this.xm;
    var b_0 = Math_0.max(0, b);
    return Math_0.min(tmp$, b_0);
  };
  Agent.prototype.xmBarPercent_0 = function () {
    return (this.calcAbsXmBar_0() * 100 | 0) / this.xmCapacity() | 0;
  };
  Agent.prototype.isXmBarEmpty_0 = function () {
    return this.xmBarPercent_0() === 0;
  };
  Agent.prototype.isXmFilled = function () {
    return this.xmBarPercent_0() >= 80;
  };
  Agent.prototype.keySet = function () {
    var $receiver = this.inventory.findUniqueKeys();
    return $receiver != null ? $receiver : emptyList();
  };
  Agent.prototype.removeXm_za3lpa$ = function (v) {
    this.xm = Util_getInstance().clip_qt1dr2$(this.xm - v | 0, 0, this.xmCapacity());
  };
  Agent.prototype.addXm_za3lpa$ = function (v) {
    this.xm = Util_getInstance().clip_qt1dr2$(this.xm + v | 0, 0, this.xmCapacity());
  };
  Agent.prototype.addAp_za3lpa$ = function (v) {
    this.ap = this.ap + (v * 10 | 0) | 0;
  };
  Agent.prototype.act = function () {
    var tmp$;
    var tmp$_0;
    if ((tmp$_0 = this.action.item) != null ? tmp$_0.equals(ActionItem$Companion_getInstance().ATTACK) : null)
      tmp$ = this.attackPortal_6taknv$(false);
    else {
      var tmp$_1;
      if ((tmp$_1 = this.action.item) != null ? tmp$_1.equals(ActionItem$Companion_getInstance().DEPLOY) : null)
        tmp$ = this.deployPortal_6taknv$(false);
      else {
        var tmp$_2;
        if ((tmp$_2 = this.action.item) != null ? tmp$_2.equals(ActionItem$Companion_getInstance().MOVE) : null)
          tmp$ = this.moveCloserToDestinationPortal_0();
        else if (this.action.isBusy())
          tmp$ = this;
        else
          tmp$ = ActionSelector_getInstance().doSomethingElse_912u9o$(this);
      }
    }
    var next = tmp$;
    next.collectXm_0();
    return next;
  };
  function Agent$moveElsewhere$lambda$lambda(closure$agent) {
    return function () {
      return MovementUtil_getInstance().moveToRandomPortal_912u9o$(closure$agent);
    };
  }
  function Agent$moveElsewhere$lambda$lambda_0(closure$agent) {
    return function () {
      return MovementUtil_getInstance().moveToNearestPortal_912u9o$(closure$agent);
    };
  }
  function Agent$moveElsewhere$lambda$lambda_1(closure$agent) {
    return function () {
      return MovementUtil_getInstance().moveToUncapturedPortal_912u9o$(closure$agent);
    };
  }
  function Agent$moveElsewhere$lambda$lambda_2(closure$agent) {
    return function () {
      return MovementUtil_getInstance().moveToFriendlyHighLevelPortal_912u9o$(closure$agent);
    };
  }
  function Agent$moveElsewhere$lambda$lambda_3(closure$agent) {
    return function () {
      return MovementUtil_getInstance().attackClosePortal_912u9o$(closure$agent);
    };
  }
  function Agent$moveElsewhere$lambda$lambda_4(closure$agent) {
    return function () {
      return MovementUtil_getInstance().attackMostVulnerablePortal_912u9o$(closure$agent);
    };
  }
  function Agent$moveElsewhere$lambda$lambda_5(closure$agent) {
    return function () {
      return MovementUtil_getInstance().attackMostLinkedPortal_912u9o$(closure$agent);
    };
  }
  function Agent$moveElsewhere$lambda$lambda_6(closure$agent) {
    return function () {
      return MovementUtil_getInstance().moveToNearestPortal_912u9o$(closure$agent);
    };
  }
  Agent.prototype.moveElsewhere = function () {
    var agent = this;
    var hasEnemyPortals = MovementUtil_getInstance().hasEnemyPortals_912u9o$(agent);
    var $receiver = QDestinations_getInstance();
    var randomQ = ActionSelector_getInstance().q_aafct0$(this.faction, $receiver.MOVE_TO_RANDOM);
    var nearQ = ActionSelector_getInstance().q_aafct0$(this.faction, $receiver.MOVE_TO_NEAR);
    var uncapturedQ = MovementUtil_getInstance().hasUncapturedPortals() ? ActionSelector_getInstance().q_aafct0$(this.faction, $receiver.MOVE_TO_UNCAPTURED) : -1.0;
    var friendlyQ = MovementUtil_getInstance().hasFriendlyPortals_912u9o$(agent) ? ActionSelector_getInstance().q_aafct0$(this.faction, $receiver.MOVE_TO_MOST_FRIENDLY) : -1.0;
    var nearEnemyQ = this.hasXmps_0() && hasEnemyPortals ? ActionSelector_getInstance().q_aafct0$(this.faction, $receiver.MOVE_TO_NEAR_ENEMY) : -1.0;
    var weakEnemyQ = this.hasXmps_0() && hasEnemyPortals ? ActionSelector_getInstance().q_aafct0$(this.faction, $receiver.MOVE_TO_WEAK_ENEMY) : -1.0;
    var strongEnemyQ = this.hasXmps_0() && hasEnemyPortals ? ActionSelector_getInstance().q_aafct0$(this.faction, $receiver.MOVE_TO_STRONG_ENEMY) : -1.0;
    var qValues = listOf([to(randomQ, Agent$moveElsewhere$lambda$lambda(agent)), to(nearQ, Agent$moveElsewhere$lambda$lambda_0(agent)), to(uncapturedQ, Agent$moveElsewhere$lambda$lambda_1(agent)), to(friendlyQ, Agent$moveElsewhere$lambda$lambda_2(agent)), to(nearEnemyQ, Agent$moveElsewhere$lambda$lambda_3(agent)), to(weakEnemyQ, Agent$moveElsewhere$lambda$lambda_4(agent)), to(strongEnemyQ, Agent$moveElsewhere$lambda$lambda_5(agent))]);
    var newAgent = Util_getInstance().select_4u7aq8$(qValues, Agent$moveElsewhere$lambda$lambda_6(agent))();
    newAgent.action.start_fyi6w8$(ActionItem$Companion_getInstance().MOVE);
    return newAgent;
  };
  Agent.prototype.updateLastPos = function () {
    var distance = this.pos.distanceTo_lfj9be$(this.lastPosition_0);
    var isStuck = distance <= Dim_getInstance().maxDeploymentRange;
    if (isStuck) {
      var newDest = World_getInstance().randomPortal();
      var dist = this.skills.deployPrecision * Dim_getInstance().maxDeploymentRange;
      this.actionPortal = newDest;
      this.destination = newDest.findRandomPointNearPortal_za3lpa$(numberToInt(dist));
    }
    this.lastPosition_0 = this.pos;
  };
  Agent.prototype.moveCloserToDestinationPortal_0 = function () {
    var tmp$;
    if (!World_getInstance().isReady) {
      console.warn('World is not ready.');
      return this.doNothing();
    }
    if (this.isAtActionPortal()) {
      this.action.end();
      return this;
    }
    var force = (tmp$ = this.actionPortal.vectorField.get_11rb$(this.pos.toShadowPos())) != null ? tmp$ : Complex$Companion_getInstance().ZERO;
    this.velocity = MovementUtil_getInstance().move_ovcmsq$(this.velocity, force, this.skills.speed);
    return this.copy_8zq494$(void 0, void 0, Coords_init(numberToInt(this.pos.x + this.velocity.re), numberToInt(this.pos.y + this.velocity.im)));
  };
  Agent.prototype.hasXmps_0 = function () {
    return !this.inventory.findXmps().isEmpty();
  };
  Agent.prototype.isArrived_0 = function () {
    return this.distanceToDestination_0() <= this.skills.inRangeSpeed();
  };
  Agent.prototype.moveCloserInRange_0 = function () {
    return this.moveCloserTo_0(this.destination);
  };
  Agent.prototype.moveCloserTo_0 = function (dest) {
    var part = this.skills.inRangeSpeed() / this.pos.distanceTo_lfj9be$(dest);
    var rawDiffX = numberToInt(this.pos.xDiff_lfj9be$(dest) * part);
    var rawDiffY = numberToInt(this.pos.yDiff_lfj9be$(dest) * part);
    var rawNextX = this.pos.x - rawDiffX;
    var rawNextY = this.pos.y - rawDiffY;
    return this.copy_8zq494$(void 0, void 0, new Coords(rawNextX, rawNextY));
  };
  Agent.prototype.collectXm_0 = function () {
    var heaps = XmMap_getInstance().findXmInRange_lfj9be$(this.pos);
    var tmp$;
    tmp$ = heaps.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (this.xm < this.xmCapacity()) {
        this.addXm_za3lpa$(element.value.xm);
        element.value.collect();
      }
    }
  };
  function Agent$attackPortal$findExactDestination(this$Agent) {
    return function () {
      var tmp$;
      if (this$Agent.actionPortal.calcHealth() > 0.8) {
        return this$Agent.actionPortal.location;
      }
      var maybeDestination = this$Agent.actionPortal.findStrongestResoPos();
      var isPassable = maybeDestination != null && maybeDestination.isPassable();
      if (isPassable) {
        tmp$ = ensureNotNull(maybeDestination);
      }
       else {
        tmp$ = this$Agent.actionPortal.location;
      }
      return tmp$;
    };
  }
  Agent.prototype.attackPortal_6taknv$ = function (isFirst) {
    var tmp$;
    if (isFirst) {
      this.action.start_fyi6w8$(ActionItem$Companion_getInstance().ATTACK);
      var findExactDestination = Agent$attackPortal$findExactDestination(this);
      var inRangePosition = findExactDestination();
      this.destination = inRangePosition;
    }
    if (!this.isArrived_0() && this.action.isBusy())
      tmp$ = this.moveCloserInRange_0();
    else {
      if (Attacker_getInstance().isActionPossible_912u9o$(this)) {
        tmp$ = Attacker_getInstance().performAction_912u9o$(this);
      }
       else {
        tmp$ = this.doNothing();
      }
    }
    return tmp$;
  };
  Agent.prototype.capturePortal_6taknv$ = function (isFirst) {
    return this.deployPortal_6taknv$(isFirst);
  };
  Agent.prototype.deployPortal_6taknv$ = function (isFirst) {
    var tmp$;
    if (isFirst) {
      this.action.start_fyi6w8$(ActionItem$Companion_getInstance().DEPLOY);
      var a = Dim_getInstance().minDeploymentRange;
      var b = Dim_getInstance().maxDeploymentRange * Util_getInstance().random() * this.skills.deployPrecision;
      var distance = numberToInt(Math_0.max(a, b));
      var dest = this.actionPortal.findRandomPointNearPortal_za3lpa$(distance);
      this.destination = dest;
    }
    if (!this.isArrived_0() && this.action.isBusy())
      tmp$ = this.moveCloserInRange_0();
    else {
      if (Deployer_getInstance().isActionPossible_912u9o$(this)) {
        tmp$ = Deployer_getInstance().performAction_912u9o$(this);
      }
       else {
        tmp$ = this.doNothing();
      }
    }
    return tmp$;
  };
  Agent.prototype.doNothing = function () {
    this.action.start_fyi6w8$(ActionItem$Companion_getInstance().WAIT);
    return this;
  };
  function Agent$findPortalsInAttackRange$lambda(this$Agent) {
    return function (it) {
      return it.location.distanceTo_lfj9be$(this$Agent.pos);
    };
  }
  var compareBy$lambda_0 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_2(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_2.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_2.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Agent.prototype.findPortalsInAttackRange_0 = function (level) {
    var attackDistance = level.rangeM * 0.5 + Dim_getInstance().portalRadius;
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var tmp$_0;
      if (!equals((tmp$_0 = element.owner) != null ? tmp$_0.faction : null, this.faction))
        destination.add_11rb$(element);
    }
    var enemyPortals = destination;
    var destination_0 = ArrayList_init_0();
    var tmp$_1;
    tmp$_1 = enemyPortals.iterator();
    while (tmp$_1.hasNext()) {
      var element_0 = tmp$_1.next();
      if (element_0.location.distanceTo_lfj9be$(this.pos) <= attackDistance)
        destination_0.add_11rb$(element_0);
    }
    return sortedWith(destination_0, new Comparator$ObjectLiteral_2(compareBy$lambda_0(Agent$findPortalsInAttackRange$lambda(this))));
  };
  var addAll = Kotlin.kotlin.collections.addAll_ipc267$;
  Agent.prototype.findResosInAttackRange_3vxbq7$ = function (level) {
    var attackDistance = level.rangeM * 0.5;
    var portals = this.findPortalsInAttackRange_0(level);
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = portals.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var $receiver = element.resoSlots;
      var destination_0 = ArrayList_init($receiver.size);
      var tmp$_0;
      tmp$_0 = $receiver.entries.iterator();
      while (tmp$_0.hasNext()) {
        var item = tmp$_0.next();
        destination_0.add_11rb$(item.value);
      }
      var list = destination_0;
      addAll(destination, list);
    }
    var slots = destination;
    var destination_1 = ArrayList_init_0();
    var tmp$_1;
    tmp$_1 = slots.iterator();
    while (tmp$_1.hasNext()) {
      var element_0 = tmp$_1.next();
      var tmp$_2, tmp$_3, tmp$_4;
      if (((tmp$_4 = (tmp$_3 = (tmp$_2 = element_0.resonator) != null ? tmp$_2.coords : null) != null ? tmp$_3.distanceTo_lfj9be$(this.pos) : null) != null ? tmp$_4 : attackDistance * 2) <= attackDistance)
        destination_1.add_11rb$(element_0);
    }
    var resosInRange = destination_1;
    var destination_2 = ArrayList_init(collectionSizeOrDefault(resosInRange, 10));
    var tmp$_5;
    tmp$_5 = resosInRange.iterator();
    while (tmp$_5.hasNext()) {
      var item_0 = tmp$_5.next();
      destination_2.add_11rb$(item_0.resonator);
    }
    return filterNotNull(destination_2);
  };
  Agent.prototype.draw_f69bme$ = function (ctx) {
    if (HtmlUtil_getInstance().isNotRunningInBrowser())
      return;
    var image = ActionItem$Companion_getInstance().getIcon_5bvev3$(this.action.item, this.faction);
    ctx.drawImage(image, this.pos.x, this.pos.y);
    var xmBar = Agent$Companion_getInstance().getXmBarImage_0(this.faction, this.xmBarPercent_0());
    ctx.drawImage(xmBar, this.pos.x, this.pos.y - 3);
  };
  Agent.prototype.drawRadius_f69bme$ = function (ctx) {
    if (HtmlUtil_getInstance().isNotRunningInBrowser())
      return;
    if (Styles_getInstance().isDrawDestination) {
      DrawUtil_getInstance().drawLine_ovbgws$(ctx, this.lineToPortal_0(this.actionPortal), Colors_getInstance().nextPortal, 1.0);
      DrawUtil_getInstance().drawLine_ovbgws$(ctx, this.lineToDestination_0(), Colors_getInstance().destination, 1.0);
    }
    if (Styles_getInstance().isDrawAgentRange) {
      var deployCircle = new Circle(this.pos, Dim_getInstance().maxDeploymentRange);
      DrawUtil_getInstance().drawCircle_3kie0f$(ctx, deployCircle, Colors_getInstance().agentDeployCircle, Dim_getInstance().agentDeployCircleLineWidth);
    }
  };
  Agent.prototype.toString = function () {
    return 'L' + toString(this.getLevel()) + ' ' + this.faction.abbr + '-' + this.name;
  };
  Agent.prototype.equals = function (other) {
    return Kotlin.isType(other, Agent) && equals(this.key(), other.key());
  };
  Agent.prototype.hashCode = function () {
    return hashCode(this.key()) * 31 | 0;
  };
  function Agent$Companion() {
    Agent$Companion_instance = this;
    var tmp$;
    if (HtmlUtil_getInstance().isRunningInBrowser()) {
      var $receiver = Faction$values();
      var destination = ArrayList_init_0();
      var tmp$_0;
      for (tmp$_0 = 0; tmp$_0 !== $receiver.length; ++tmp$_0) {
        var element = $receiver[tmp$_0];
        var $receiver_0 = new IntRange(0, 100);
        var destination_0 = ArrayList_init(collectionSizeOrDefault($receiver_0, 10));
        var tmp$_1;
        tmp$_1 = $receiver_0.iterator();
        while (tmp$_1.hasNext()) {
          var item = tmp$_1.next();
          var tmp$_2 = destination_0.add_11rb$;
          var lw = Dim_getInstance().agentLineWidth;
          var r = Dim_getInstance().agentRadius;
          var w = (r + lw | 0) * 2 | 0;
          tmp$_2.call(destination_0, to(this.xmKey_0(element, item), DrawUtil_getInstance().renderBarImage_ewpgoy$(element.color, item, 3, w, lw)));
        }
        var list = destination_0;
        addAll(destination, list);
      }
      tmp$ = toMap(destination);
    }
     else {
      tmp$ = emptyMap();
    }
    this.xmBarImages_0 = tmp$;
  }
  Agent$Companion.prototype.xmCapacity_0 = function (level) {
    switch (level) {
      case 1:
        return 3000;
      case 2:
        return 4000;
      case 3:
        return 5000;
      case 4:
        return 6000;
      case 5:
        return 7000;
      case 6:
        return 8000;
      case 7:
        return 9000;
      case 8:
        return 10000;
      case 9:
        return 10900;
      case 10:
        return 11700;
      case 11:
        return 12400;
      case 12:
        return 13000;
      case 13:
        return 13500;
      case 14:
        return 13900;
      case 15:
        return 14200;
      default:return 14400;
    }
  };
  Agent$Companion.prototype.getLevel_0 = function (actionPoints) {
    if (actionPoints >= 0 && actionPoints <= 10000)
      return 1;
    else if (actionPoints >= 10000 && actionPoints <= 30000)
      return 2;
    else if (actionPoints >= 30000 && actionPoints <= 70000)
      return 3;
    else if (actionPoints >= 70000 && actionPoints <= 150000)
      return 4;
    else if (actionPoints >= 150000 && actionPoints <= 300000)
      return 5;
    else if (actionPoints >= 300000 && actionPoints <= 600000)
      return 6;
    else if (actionPoints >= 600000 && actionPoints <= 1200000)
      return 7;
    else if (actionPoints >= 1200000 && actionPoints <= 2400000)
      return 8;
    else if (actionPoints >= 2400000 && actionPoints <= 4000000)
      return 9;
    else if (actionPoints >= 4000000 && actionPoints <= 6000000)
      return 10;
    else if (actionPoints >= 6000000 && actionPoints <= 8400000)
      return 11;
    else if (actionPoints >= 8400000 && actionPoints <= 12000000)
      return 12;
    else if (actionPoints >= 12000000 && actionPoints <= 17000000)
      return 13;
    else if (actionPoints >= 17000000 && actionPoints <= 24000000)
      return 14;
    else if (actionPoints >= 24000000 && actionPoints <= 40000000)
      return 15;
    else
      return 16;
  };
  Agent$Companion.prototype.getLinkingRange_0 = function (level) {
    switch (level) {
      case 9:
        return 2250;
      case 10:
        return 2500;
      case 11:
        return 2750;
      case 12:
        return 3000;
      case 13:
        return 3250;
      case 14:
        return 3500;
      case 15:
        return 3750;
      case 16:
        return 4000;
      default:return 2000;
    }
  };
  Agent$Companion.prototype.xmKey_0 = function (faction, percent) {
    return faction.abbr + ':' + toString(percent);
  };
  Agent$Companion.prototype.getXmBarImage_0 = function (faction, percent) {
    if (!(0 <= percent && percent <= 100)) {
      var message = 'Check failed.';
      throw IllegalStateException_init(message.toString());
    }
    return getValue(this.xmBarImages_0, this.xmKey_0(faction, percent));
  };
  Agent$Companion.prototype.initialActionPortal_0 = function (coords) {
    var tmp$;
    return HtmlUtil_getInstance().isRunningInBrowser() ? (tmp$ = Util_getInstance().findNearestPortal_lfj9be$(coords)) != null ? tmp$ : World_getInstance().allPortals.get_za3lpa$(0) : Portal$Companion_getInstance().create_lfj9be$(coords);
  };
  Agent$Companion.prototype.createFrog_5edep5$ = function (grid) {
    return this.create_0(grid, Faction$ENL_getInstance());
  };
  Agent$Companion.prototype.createSmurf_5edep5$ = function (grid) {
    return this.create_0(grid, Faction$RES_getInstance());
  };
  Agent$Companion.prototype.create_0 = function (grid, faction) {
    var ap = Config_getInstance().initialAp();
    var initialXm = this.xmCapacity_0(this.getLevel_0(ap));
    var coords = Coords$Companion_getInstance().createRandomPassable_5edep5$(grid);
    var actionPortal = this.initialActionPortal_0(coords);
    var agent = new Agent(faction, Util_getInstance().generateAgentName(), coords, Skills$Companion_getInstance().createRandom(), Inventory$Companion_getInstance().empty(), Action$Companion_getInstance().create(), actionPortal, actionPortal.location, coords, ap, initialXm);
    if (HtmlUtil_getInstance().isQuickstart()) {
      agent.inventory.items.addAll_brywnq$(Inventory$Companion_getInstance().quickStart_912u9o$(agent));
    }
    return agent;
  };
  Agent$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Agent$Companion_instance = null;
  function Agent$Companion_getInstance() {
    if (Agent$Companion_instance === null) {
      new Agent$Companion();
    }
    return Agent$Companion_instance;
  }
  Agent.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Agent',
    interfaces: []
  };
  Agent.prototype.component1 = function () {
    return this.faction;
  };
  Agent.prototype.component2 = function () {
    return this.name;
  };
  Agent.prototype.component3 = function () {
    return this.pos;
  };
  Agent.prototype.component4 = function () {
    return this.skills;
  };
  Agent.prototype.component5 = function () {
    return this.inventory;
  };
  Agent.prototype.component6 = function () {
    return this.action;
  };
  Agent.prototype.component7 = function () {
    return this.actionPortal;
  };
  Agent.prototype.component8 = function () {
    return this.destination;
  };
  Agent.prototype.component9_0 = function () {
    return this.lastPosition_0;
  };
  Agent.prototype.component10 = function () {
    return this.ap;
  };
  Agent.prototype.component11 = function () {
    return this.xm;
  };
  Agent.prototype.component12 = function () {
    return this.velocity;
  };
  Agent.prototype.copy_8zq494$ = function (faction, name, pos, skills, inventory, action, actionPortal, destination, lastPosition, ap, xm, velocity) {
    return new Agent(faction === void 0 ? this.faction : faction, name === void 0 ? this.name : name, pos === void 0 ? this.pos : pos, skills === void 0 ? this.skills : skills, inventory === void 0 ? this.inventory : inventory, action === void 0 ? this.action : action, actionPortal === void 0 ? this.actionPortal : actionPortal, destination === void 0 ? this.destination : destination, lastPosition === void 0 ? this.lastPosition_0 : lastPosition, ap === void 0 ? this.ap : ap, xm === void 0 ? this.xm : xm, velocity === void 0 ? this.velocity : velocity);
  };
  function AgentSize(offset) {
    AgentSize$Companion_getInstance();
    this.offset = offset;
  }
  function AgentSize$Companion() {
    AgentSize$Companion_instance = this;
  }
  AgentSize$Companion.prototype.randomOffset_0 = function () {
    var rand = Util_getInstance().random();
    return rand < 0.03 ? 1 : 0;
  };
  AgentSize$Companion.prototype.createRandom = function () {
    return new AgentSize(this.randomOffset_0());
  };
  AgentSize$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var AgentSize$Companion_instance = null;
  function AgentSize$Companion_getInstance() {
    if (AgentSize$Companion_instance === null) {
      new AgentSize$Companion();
    }
    return AgentSize$Companion_instance;
  }
  AgentSize.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'AgentSize',
    interfaces: []
  };
  AgentSize.prototype.component1 = function () {
    return this.offset;
  };
  AgentSize.prototype.copy_za3lpa$ = function (offset) {
    return new AgentSize(offset === void 0 ? this.offset : offset);
  };
  AgentSize.prototype.toString = function () {
    return 'AgentSize(offset=' + Kotlin.toString(this.offset) + ')';
  };
  AgentSize.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.offset) | 0;
    return result;
  };
  AgentSize.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.offset, other.offset))));
  };
  function Faction(name, ordinal, abbr, nickName, color, fieldStyle) {
    Enum.call(this);
    this.abbr = abbr;
    this.nickName = nickName;
    this.color = color;
    this.fieldStyle = fieldStyle;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function Faction_initFields() {
    Faction_initFields = function () {
    };
    Faction$NONE_instance = new Faction('NONE', 0, 'NONE', 'None', '#FFFFFF', 'rgba(255, 255, 255, ');
    Faction$ENL_instance = new Faction('ENL', 1, 'ENL', 'Frog', '#03DC03', 'rgba(3, 220, 3, ');
    Faction$RES_instance = new Faction('RES', 2, 'RES', 'Smurf', '#0088FF', 'rgba(0, 136, 255, ');
    Faction$Companion_getInstance();
  }
  var Faction$NONE_instance;
  function Faction$NONE_getInstance() {
    Faction_initFields();
    return Faction$NONE_instance;
  }
  var Faction$ENL_instance;
  function Faction$ENL_getInstance() {
    Faction_initFields();
    return Faction$ENL_instance;
  }
  var Faction$RES_instance;
  function Faction$RES_getInstance() {
    Faction_initFields();
    return Faction$RES_instance;
  }
  Faction.prototype.isEnemy_bip15f$ = function (faction) {
    return faction === Faction$ENL_getInstance() && this === Faction$RES_getInstance() || (faction === Faction$RES_getInstance() && this === Faction$ENL_getInstance());
  };
  Faction.prototype.enemy = function () {
    switch (this.name) {
      case 'ENL':
        return Faction$RES_getInstance();
      case 'RES':
        return Faction$ENL_getInstance();
      default:return Faction$NONE_getInstance();
    }
  };
  function Faction$Companion() {
    Faction$Companion_instance = this;
  }
  Faction$Companion.prototype.fromString_pdl1vj$ = function (s) {
    switch (s != null ? s.toUpperCase() : null) {
      case 'RES':
        return Faction$RES_getInstance();
      case 'ENL':
        return Faction$ENL_getInstance();
      case 'NONE':
        return Faction$NONE_getInstance();
      default:return null;
    }
  };
  Faction$Companion.prototype.all = function () {
    return listOf([Faction$ENL_getInstance(), Faction$RES_getInstance()]);
  };
  Faction$Companion.prototype.random = function () {
    return Util_getInstance().random() < 0.5 ? Faction$ENL_getInstance() : Faction$RES_getInstance();
  };
  Faction$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Faction$Companion_instance = null;
  function Faction$Companion_getInstance() {
    Faction_initFields();
    if (Faction$Companion_instance === null) {
      new Faction$Companion();
    }
    return Faction$Companion_instance;
  }
  Faction.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Faction',
    interfaces: [Enum]
  };
  function Faction$values() {
    return [Faction$NONE_getInstance(), Faction$ENL_getInstance(), Faction$RES_getInstance()];
  }
  Faction.values = Faction$values;
  function Faction$valueOf(name) {
    switch (name) {
      case 'NONE':
        return Faction$NONE_getInstance();
      case 'ENL':
        return Faction$ENL_getInstance();
      case 'RES':
        return Faction$RES_getInstance();
      default:throwISE('No enum constant agent.Faction.' + name);
    }
  }
  Faction.valueOf_61zpoe$ = Faction$valueOf;
  function Inventory(items) {
    Inventory$Companion_getInstance();
    if (items === void 0) {
      items = ArrayList_init_0();
    }
    this.items = items;
  }
  Inventory.prototype.findKeys = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, PortalKey))
        destination.add_11rb$(element);
    }
    var destination_0 = ArrayList_init(collectionSizeOrDefault(destination, 10));
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      var tmp$_1;
      destination_0.add_11rb$(Kotlin.isType(tmp$_1 = item, PortalKey) ? tmp$_1 : throwCCE());
    }
    return destination_0;
  };
  Inventory.prototype.findXmps = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, XmpBurster))
        destination.add_11rb$(element);
    }
    var destination_0 = ArrayList_init(collectionSizeOrDefault(destination, 10));
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      var tmp$_1;
      destination_0.add_11rb$(Kotlin.isType(tmp$_1 = item, XmpBurster) ? tmp$_1 : throwCCE());
    }
    return destination_0;
  };
  Inventory.prototype.findResonators = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, Resonator))
        destination.add_11rb$(element);
    }
    var destination_0 = ArrayList_init(collectionSizeOrDefault(destination, 10));
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      var tmp$_1;
      destination_0.add_11rb$(Kotlin.isType(tmp$_1 = item, Resonator) ? tmp$_1 : throwCCE());
    }
    return destination_0;
  };
  Inventory.prototype.findPowerCubes = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, PowerCube))
        destination.add_11rb$(element);
    }
    var destination_0 = ArrayList_init(collectionSizeOrDefault(destination, 10));
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      var tmp$_1;
      destination_0.add_11rb$(Kotlin.isType(tmp$_1 = item, PowerCube) ? tmp$_1 : throwCCE());
    }
    return destination_0;
  };
  Inventory.prototype.findShields = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, Shield))
        destination.add_11rb$(element);
    }
    var destination_0 = ArrayList_init(collectionSizeOrDefault(destination, 10));
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      var tmp$_1;
      destination_0.add_11rb$(Kotlin.isType(tmp$_1 = item, Shield) ? tmp$_1 : throwCCE());
    }
    return destination_0;
  };
  Inventory.prototype.findUniqueKeys = function () {
    return distinct(this.findKeys());
  };
  Inventory.prototype.addItem_1dq9g6$ = function (item) {
    return this.items.add_11rb$(item);
  };
  Inventory.prototype.addItems_bbrmyp$ = function (newItems) {
    return this.items.addAll_brywnq$(newItems);
  };
  Inventory.prototype.consumeKeyToPortal_hv9zn6$ = function (portal) {
    var tmp$;
    var $receiver = ensureNotNull(this.findUniqueKeys());
    var firstOrNull$result;
    firstOrNull$break: do {
      var tmp$_0;
      tmp$_0 = $receiver.iterator();
      while (tmp$_0.hasNext()) {
        var element = tmp$_0.next();
        var tmp$_1;
        if ((tmp$_1 = element.portal) != null ? tmp$_1.equals(portal) : null) {
          firstOrNull$result = element;
          break firstOrNull$break;
        }
      }
      firstOrNull$result = null;
    }
     while (false);
    tmp$ = firstOrNull$result;
    if (tmp$ == null) {
      throw IllegalStateException_init('Key should exist.');
    }
    var key = tmp$;
    this.items.remove_11rb$(key);
  };
  Inventory.prototype.consumeXmps_ss5kb$ = function (xmps) {
    return this.items.removeAll_brywnq$(xmps);
  };
  Inventory.prototype.consumeResos_tvxik5$ = function (resos) {
    return this.items.removeAll_brywnq$(resos);
  };
  Inventory.prototype.consumeCubes_lz36jp$ = function (cubes) {
    return this.items.removeAll_brywnq$(cubes);
  };
  Inventory.prototype.keyCount = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, PortalKey))
        destination.add_11rb$(element);
    }
    return destination.size;
  };
  Inventory.prototype.xmpCount_0 = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, XmpBurster))
        destination.add_11rb$(element);
    }
    return destination.size;
  };
  Inventory.prototype.usCount_0 = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, UltraStrike))
        destination.add_11rb$(element);
    }
    return destination.size;
  };
  Inventory.prototype.weaponCount_0 = function () {
    return this.xmpCount_0() + this.usCount_0() | 0;
  };
  Inventory.prototype.resoCount_0 = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, Resonator))
        destination.add_11rb$(element);
    }
    return destination.size;
  };
  Inventory.prototype.shieldCount_0 = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, Shield))
        destination.add_11rb$(element);
    }
    return destination.size;
  };
  Inventory.prototype.powerCubeCount_0 = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, PowerCube))
        destination.add_11rb$(element);
    }
    return destination.size;
  };
  function Inventory$toString$lambda(it) {
    return it;
  }
  var Grouping = Kotlin.kotlin.collections.Grouping;
  function groupingBy$ObjectLiteral(this$groupingBy, closure$keySelector) {
    this.this$groupingBy = this$groupingBy;
    this.closure$keySelector = closure$keySelector;
  }
  groupingBy$ObjectLiteral.prototype.sourceIterator = function () {
    return this.this$groupingBy.iterator();
  };
  groupingBy$ObjectLiteral.prototype.keyOf_11rb$ = function (element) {
    return this.closure$keySelector(element);
  };
  groupingBy$ObjectLiteral.$metadata$ = {kind: Kind_CLASS, interfaces: [Grouping]};
  Inventory.prototype.toString = function () {
    var $receiver = this.items;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (!(Kotlin.isType(element, PortalKey) || Kotlin.isType(element, XmpBurster) || Kotlin.isType(element, Resonator)))
        destination.add_11rb$(element);
    }
    var filtered = destination;
    var $receiver_0 = eachCount(new groupingBy$ObjectLiteral(filtered, Inventory$toString$lambda));
    var destination_0 = ArrayList_init($receiver_0.size);
    var tmp$_0;
    tmp$_0 = $receiver_0.entries.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      var tmp$_1 = destination_0.add_11rb$;
      var transform$result;
      var count = item.value;
      if (count === 1) {
        transform$result = item.key.toString();
      }
       else {
        transform$result = count.toString() + 'x' + toString(item.key);
      }
      tmp$_1.call(destination_0, transform$result);
    }
    var items = destination_0.toString();
    return this.keyCount().toString() + ' keys ' + items;
  };
  function Inventory$Companion() {
    Inventory$Companion_instance = this;
  }
  Inventory$Companion.prototype.empty = function () {
    return new Inventory();
  };
  Inventory$Companion.prototype.quickStart_912u9o$ = function (agent) {
    var level = agent.getLevel();
    var $receiver = new IntRange(1, 20);
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(XmpBurster$Companion_getInstance().create_2b7tta$(agent, level));
    }
    var $receiver_0 = new IntRange(1, 30);
    var destination_0 = ArrayList_init(collectionSizeOrDefault($receiver_0, 10));
    var tmp$_0;
    tmp$_0 = $receiver_0.iterator();
    while (tmp$_0.hasNext()) {
      var item_0 = tmp$_0.next();
      destination_0.add_11rb$(XmpBurster$Companion_getInstance().create_2b7tta$(agent, level - 1 | 0));
    }
    var $receiver_1 = new IntRange(1, 10);
    var destination_1 = ArrayList_init(collectionSizeOrDefault($receiver_1, 10));
    var tmp$_1;
    tmp$_1 = $receiver_1.iterator();
    while (tmp$_1.hasNext()) {
      var item_1 = tmp$_1.next();
      destination_1.add_11rb$(Resonator$Companion_getInstance().create_2b7tta$(agent, level));
    }
    var $receiver_2 = new IntRange(1, 10);
    var destination_2 = ArrayList_init(collectionSizeOrDefault($receiver_2, 10));
    var tmp$_2;
    tmp$_2 = $receiver_2.iterator();
    while (tmp$_2.hasNext()) {
      var item_2 = tmp$_2.next();
      destination_2.add_11rb$(Resonator$Companion_getInstance().create_2b7tta$(agent, level - 1 | 0));
    }
    var $receiver_3 = new IntRange(1, 20);
    var destination_3 = ArrayList_init(collectionSizeOrDefault($receiver_3, 10));
    var tmp$_3;
    tmp$_3 = $receiver_3.iterator();
    while (tmp$_3.hasNext()) {
      var item_3 = tmp$_3.next();
      destination_3.add_11rb$(Resonator$Companion_getInstance().create_2b7tta$(agent, level - 2 | 0));
    }
    var $receiver_4 = new IntRange(1, 30);
    var destination_4 = ArrayList_init(collectionSizeOrDefault($receiver_4, 10));
    var tmp$_4;
    tmp$_4 = $receiver_4.iterator();
    while (tmp$_4.hasNext()) {
      var item_4 = tmp$_4.next();
      destination_4.add_11rb$(Resonator$Companion_getInstance().create_2b7tta$(agent, level - 3 | 0));
    }
    var $receiver_5 = new IntRange(1, 20);
    var destination_5 = ArrayList_init(collectionSizeOrDefault($receiver_5, 10));
    var tmp$_5;
    tmp$_5 = $receiver_5.iterator();
    while (tmp$_5.hasNext()) {
      var item_5 = tmp$_5.next();
      destination_5.add_11rb$(PowerCube$Companion_getInstance().create_2b7tta$(agent, level));
    }
    var $receiver_6 = new IntRange(1, 10);
    var destination_6 = ArrayList_init(collectionSizeOrDefault($receiver_6, 10));
    var tmp$_6;
    tmp$_6 = $receiver_6.iterator();
    while (tmp$_6.hasNext()) {
      var item_6 = tmp$_6.next();
      destination_6.add_11rb$(PowerCube$Companion_getInstance().create_2b7tta$(agent, level - 1 | 0));
    }
    return flatten(listOf([destination, destination_0, destination_1, destination_2, destination_3, destination_4, destination_5, destination_6]));
  };
  Inventory$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Inventory$Companion_instance = null;
  function Inventory$Companion_getInstance() {
    if (Inventory$Companion_instance === null) {
      new Inventory$Companion();
    }
    return Inventory$Companion_instance;
  }
  Inventory.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Inventory',
    interfaces: []
  };
  Inventory.prototype.component1 = function () {
    return this.items;
  };
  Inventory.prototype.copy_m1e5jq$ = function (items) {
    return new Inventory(items === void 0 ? this.items : items);
  };
  Inventory.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.items) | 0;
    return result;
  };
  Inventory.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && Kotlin.equals(this.items, other.items))));
  };
  function MovementUtil() {
    MovementUtil_instance = this;
  }
  MovementUtil.prototype.findUncapturedPortals = function () {
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.isUncaptured())
        destination.add_11rb$(element);
    }
    return destination;
  };
  MovementUtil.prototype.hasUncapturedPortals = function () {
    return !this.findUncapturedPortals().isEmpty();
  };
  MovementUtil.prototype.findEnemyPortals_912u9o$ = function (agent) {
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.isEnemyOf_912u9o$(agent))
        destination.add_11rb$(element);
    }
    return destination;
  };
  MovementUtil.prototype.hasEnemyPortals_912u9o$ = function (agent) {
    return !this.findEnemyPortals_912u9o$(agent).isEmpty();
  };
  MovementUtil.prototype.findFriendlyPortals_912u9o$ = function (agent) {
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.isFriendlyTo_912u9o$(agent))
        destination.add_11rb$(element);
    }
    return destination;
  };
  MovementUtil.prototype.hasFriendlyPortals_912u9o$ = function (agent) {
    return !this.findFriendlyPortals_912u9o$(agent).isEmpty();
  };
  function MovementUtil$moveToUncapturedPortal$lambda(closure$agent) {
    return function (it) {
      return closure$agent.distanceToPortal_hv9zn6$(it);
    };
  }
  var compareBy$lambda_1 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_3(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_3.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_3.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  MovementUtil.prototype.moveToUncapturedPortal_912u9o$ = function (agent) {
    if (!this.hasUncapturedPortals()) {
      var message = 'Check failed.';
      throw IllegalStateException_init(message.toString());
    }
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.isUncaptured())
        destination.add_11rb$(element);
    }
    var uncaptured = sortedWith(destination, new Comparator$ObjectLiteral_3(compareBy$lambda_1(MovementUtil$moveToUncapturedPortal$lambda(agent))));
    var tmp$_0;
    tmp$_0 = uncaptured.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      if (Util_getInstance().random() < agent.skills.reliability) {
        return this.goToDestinationPortal_0(agent, element_0);
      }
    }
    return agent;
  };
  MovementUtil.prototype.moveToFriendlyHighLevelPortal_912u9o$ = function (agent) {
    var tmp$, tmp$_0;
    if (!this.hasFriendlyPortals_912u9o$(agent)) {
      var message = 'Check failed.';
      throw IllegalStateException_init(message.toString());
    }
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init_0();
    var tmp$_1;
    tmp$_1 = $receiver.iterator();
    while (tmp$_1.hasNext()) {
      var element = tmp$_1.next();
      if (element.isFriendlyTo_912u9o$(agent))
        destination.add_11rb$(element);
    }
    var friendlyPortals = destination;
    var maxBy$result;
    maxBy$break: do {
      var iterator = friendlyPortals.iterator();
      if (!iterator.hasNext()) {
        maxBy$result = null;
        break maxBy$break;
      }
      var maxElem = iterator.next();
      var maxValue = maxElem.getLevel();
      while (iterator.hasNext()) {
        var e = iterator.next();
        var v = e.getLevel();
        if (Kotlin.compareTo(maxValue, v) < 0) {
          maxElem = e;
          maxValue = v;
        }
      }
      maxBy$result = maxElem;
    }
     while (false);
    var maxLevel = (tmp$_0 = (tmp$ = maxBy$result) != null ? tmp$.getLevel() : null) != null ? tmp$_0 : PortalLevel$ZERO_getInstance();
    var destination_0 = ArrayList_init_0();
    var tmp$_2;
    tmp$_2 = friendlyPortals.iterator();
    while (tmp$_2.hasNext()) {
      var element_0 = tmp$_2.next();
      if (element_0.getLevel() === maxLevel)
        destination_0.add_11rb$(element_0);
    }
    var selection = destination_0;
    var target = selection.get_za3lpa$(numberToInt(Util_getInstance().random() * (selection.size - 1 | 0)));
    return this.goToDestinationPortal_0(agent, target);
  };
  function MovementUtil$attackClosePortal$lambda(closure$a) {
    return function (it) {
      return closure$a.distanceToPortal_hv9zn6$(it);
    };
  }
  var compareBy$lambda_2 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_4(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_4.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_4.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  MovementUtil.prototype.attackClosePortal_912u9o$ = function (a) {
    return this.goAttack_0(a, firstOrNull(sortedWith(this.findEnemyPortals_912u9o$(a), new Comparator$ObjectLiteral_4(compareBy$lambda_2(MovementUtil$attackClosePortal$lambda(a))))));
  };
  function MovementUtil$attackMostLinkedPortal$lambda(it) {
    return it.links.size;
  }
  var compareBy$lambda_3 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_5(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_5.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_5.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  MovementUtil.prototype.attackMostLinkedPortal_912u9o$ = function (a) {
    return this.goAttack_0(a, firstOrNull(sortedWith(this.findEnemyPortals_912u9o$(a), new Comparator$ObjectLiteral_5(compareBy$lambda_3(MovementUtil$attackMostLinkedPortal$lambda)))));
  };
  function MovementUtil$attackMostVulnerablePortal$lambda(it) {
    return -it.calcHealth() | 0;
  }
  var compareBy$lambda_4 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_6(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_6.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_6.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  MovementUtil.prototype.attackMostVulnerablePortal_912u9o$ = function (a) {
    return this.goAttack_0(a, firstOrNull(sortedWith(this.findEnemyPortals_912u9o$(a), new Comparator$ObjectLiteral_6(compareBy$lambda_4(MovementUtil$attackMostVulnerablePortal$lambda)))));
  };
  MovementUtil.prototype.goAttack_0 = function (agent, target) {
    var tmp$;
    if (target != null) {
      tmp$ = this.goToDestinationPortal_0(agent, target);
    }
     else {
      agent.action.end();
      tmp$ = agent;
    }
    return tmp$;
  };
  function MovementUtil$moveToNearestPortal$lambda(closure$agent) {
    return function (it) {
      return closure$agent.distanceToPortal_hv9zn6$(it);
    };
  }
  var compareBy$lambda_5 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_7(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_7.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_7.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  MovementUtil.prototype.moveToNearestPortal_912u9o$ = function (agent) {
    var target = first(sortedWith(World_getInstance().allPortals, new Comparator$ObjectLiteral_7(compareBy$lambda_5(MovementUtil$moveToNearestPortal$lambda(agent)))));
    return this.goToDestinationPortal_0(agent, target);
  };
  MovementUtil.prototype.moveToRandomPortal_912u9o$ = function (agent) {
    return this.goToDestinationPortal_0(agent, World_getInstance().randomPortal());
  };
  MovementUtil.prototype.goToDestinationPortal_0 = function (agent, destination) {
    var distance = agent.skills.deployPrecision * Dim_getInstance().maxDeploymentRange;
    var nextDest = destination.findRandomPointNearPortal_za3lpa$(numberToInt(distance));
    agent.action.start_fyi6w8$(ActionItem$Companion_getInstance().MOVE);
    return agent.copy_8zq494$(void 0, void 0, void 0, void 0, void 0, void 0, destination, nextDest);
  };
  MovementUtil.prototype.move_ovcmsq$ = function (velocity, force, limit) {
    var tmp$;
    var actualForce = !(force != null ? force.equals(Complex$Companion_getInstance().ZERO) : null) ? force : Complex$Companion_getInstance().random();
    var newVelo = velocity.plus_p4p8i0$(actualForce);
    if (newVelo.mag <= limit) {
      tmp$ = newVelo;
    }
     else {
      tmp$ = newVelo.copyWithNewMagnitude_14dthe$(limit);
    }
    return tmp$;
  };
  MovementUtil.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'MovementUtil',
    interfaces: []
  };
  var MovementUtil_instance = null;
  function MovementUtil_getInstance() {
    if (MovementUtil_instance === null) {
      new MovementUtil();
    }
    return MovementUtil_instance;
  }
  function NonFaction(pos, speed, size, destination, vectorField, busyUntil) {
    NonFaction$Companion_getInstance();
    this.pos = pos;
    this.speed = speed;
    this.size = size;
    this.destination = destination;
    this.vectorField = vectorField;
    this.busyUntil = busyUntil;
    this.swarmTendency_0 = 0.02;
    this.swarmChance_0 = this.swarmTendency_0 - this.swarmTendency_0 * 0.5 * this.size.offset;
    this.isDrunk_0 = Util_getInstance().random() <= 0.02;
    this.velocity_0 = Complex$Companion_getInstance().ZERO;
  }
  NonFaction.prototype.isOnScreen = function () {
    return this.pos.isOffGrid();
  };
  NonFaction.prototype.distanceToDestination_0 = function () {
    return this.pos.distanceTo_lfj9be$(this.destination);
  };
  NonFaction.prototype.distanceToPortal_0 = function (portal) {
    return this.pos.distanceTo_lfj9be$(portal.location);
  };
  NonFaction.prototype.isAtDestination_0 = function () {
    return this.distanceToDestination_0() < Dim_getInstance().maxDeploymentRange;
  };
  NonFaction.prototype.isBusy_0 = function (tick) {
    return tick <= this.busyUntil;
  };
  NonFaction.prototype.act = function () {
    var tmp$, tmp$_0;
    if (this.isBusy_0(World_getInstance().tick)) {
      if (Util_getInstance().random() < 0.001) {
        this.busyUntil = World_getInstance().tick;
        this.moveElsewhere_0();
      }
      return;
    }
    if (Util_getInstance().random() < 0.007) {
      this.wait_0();
    }
    if (Util_getInstance().random() < 0.015) {
      this.moveElsewhere_0();
    }
    if (this.isAtDestination_0()) {
      this.wait_0();
    }
     else {
      if (Config_getInstance().isNpcSwarming && Util_getInstance().random() < this.swarmChance_0) {
        var nearPos = this.findNearest_0().pos;
        if (nearPos.distanceTo_lfj9be$(this.pos) < Dim_getInstance().agentRadius) {
          var re = -(this.pos.x - nearPos.x);
          var im = -(this.pos.y - nearPos.y);
          var acceleration = 1.2;
          tmp$_0 = new Complex(re * acceleration, im * acceleration);
        }
         else {
          tmp$_0 = new Complex(this.pos.x, this.pos.y);
        }
      }
       else {
        tmp$_0 = (tmp$ = this.vectorField.get_11rb$(this.pos.toShadowPos())) != null ? tmp$ : Complex$Companion_getInstance().ZERO;
      }
      var force = tmp$_0;
      this.velocity_0 = MovementUtil_getInstance().move_ovcmsq$(this.velocity_0, force, this.speed);
      this.pos = new Coords(this.pos.x + this.velocity_0.re, this.pos.y + this.velocity_0.im);
    }
  };
  NonFaction.prototype.findNearest_0 = function () {
    var tmp$;
    var $receiver = World_getInstance().allNonFaction;
    var destination = ArrayList_init_0();
    var tmp$_0;
    tmp$_0 = $receiver.iterator();
    while (tmp$_0.hasNext()) {
      var element = tmp$_0.next();
      if (!(element != null ? element.equals(this) : null))
        destination.add_11rb$(element);
    }
    var minBy$result;
    minBy$break: do {
      var iterator = destination.iterator();
      if (!iterator.hasNext()) {
        minBy$result = null;
        break minBy$break;
      }
      var minElem = iterator.next();
      var minValue = minElem.pos.distanceTo_lfj9be$(this.pos);
      while (iterator.hasNext()) {
        var e = iterator.next();
        var v = e.pos.distanceTo_lfj9be$(this.pos);
        if (Kotlin.compareTo(minValue, v) > 0) {
          minElem = e;
          minValue = v;
        }
      }
      minBy$result = minElem;
    }
     while (false);
    tmp$ = minBy$result;
    if (tmp$ == null) {
      throw IllegalStateException_init('Unable to find nearest to ' + this.pos);
    }
    return tmp$;
  };
  NonFaction.prototype.wait_0 = function () {
    this.velocity_0 = Complex$Companion_getInstance().ZERO;
    this.busyUntil = World_getInstance().tick + NonFaction$Companion_getInstance().createWaitTime_0() | 0;
  };
  NonFaction.prototype.moveElsewhere_0 = function () {
    var tmp$;
    if (!this.pos.isOffScreen() && Util_getInstance().random() < 0.96) {
      tmp$ = this.moveToRandomOffscreenDestination_0();
    }
     else if (Util_getInstance().random() < 0.7) {
      tmp$ = this.moveToFarPortal_0();
    }
     else {
      tmp$ = this.moveToRandomPortal_0();
    }
    return tmp$;
  };
  NonFaction.prototype.moveToRandomOffscreenDestination_0 = function () {
    var destination = first(Util_getInstance().shuffle_bemo1h$(NonFaction$Companion_getInstance().DESTINATIONS_0));
    this.vectorField = NonFaction$Companion_getInstance().getOrCreateVectorField_lfj9be$(destination);
    this.destination = destination;
  };
  NonFaction.prototype.moveToFarPortal_0 = function () {
    var portal = NonFaction$Companion_getInstance().findFarPortal_0(this.pos);
    this.vectorField = portal.vectorField;
    this.destination = portal.location;
  };
  NonFaction.prototype.moveToRandomPortal_0 = function () {
    var randomTarget = World_getInstance().allPortals.get_za3lpa$(numberToInt(Util_getInstance().random() * (World_getInstance().allPortals.size - 1 | 0)));
    this.vectorField = randomTarget.vectorField;
    this.destination = randomTarget.location;
  };
  NonFaction.prototype.draw_f69bme$ = function (ctx) {
    ctx.drawImage(NonFaction$Companion_getInstance().image_0(this.size), this.pos.x, this.pos.y);
  };
  function NonFaction$Companion() {
    NonFaction$Companion_instance = this;
    this.changeToBeRecruited = 0.05;
    this.OFFSCREEN_DISTANCE_0 = Coords$Companion_getInstance().res * 5 | 0;
    this.DESTINATIONS_0 = listOf([Coords_init(World_getInstance().w() / 3 | 0, -this.OFFSCREEN_DISTANCE_0 | 0), Coords_init((World_getInstance().w() * 2 | 0) / 3 | 0, -this.OFFSCREEN_DISTANCE_0 | 0), Coords_init(-this.OFFSCREEN_DISTANCE_0 | 0, World_getInstance().h() / 3 | 0), Coords_init(-this.OFFSCREEN_DISTANCE_0 | 0, (World_getInstance().h() * 2 | 0) / 3 | 0), Coords_init(World_getInstance().w() + this.OFFSCREEN_DISTANCE_0 | 0, World_getInstance().h() / 3 | 0), Coords_init(World_getInstance().w() + this.OFFSCREEN_DISTANCE_0 | 0, (World_getInstance().h() * 2 | 0) / 3 | 0), Coords_init(World_getInstance().w() / 3 | 0, World_getInstance().h() + this.OFFSCREEN_DISTANCE_0 | 0), Coords_init((World_getInstance().w() * 2 | 0) / 3 | 0, World_getInstance().h() + this.OFFSCREEN_DISTANCE_0 | 0)]);
    this.OFFSCREEN_EDGES_0 = listOf([Coords_init(-this.OFFSCREEN_DISTANCE_0 | 0, -this.OFFSCREEN_DISTANCE_0 | 0), Coords_init(World_getInstance().w() + this.OFFSCREEN_DISTANCE_0 | 0, -this.OFFSCREEN_DISTANCE_0 | 0), Coords_init(-this.OFFSCREEN_DISTANCE_0 | 0, World_getInstance().h() + this.OFFSCREEN_DISTANCE_0 | 0), Coords_init(World_getInstance().w() + this.OFFSCREEN_DISTANCE_0 | 0, World_getInstance().h() + this.OFFSCREEN_DISTANCE_0 | 0)]);
    this.OFFSCREEN = plus(this.DESTINATIONS_0, Config_getInstance().useOffscreenEdgeDestinations ? this.OFFSCREEN_EDGES_0 : emptyList());
    this.fields_0 = LinkedHashMap_init();
    this.images_0 = mapOf([to(-1, this.drawTemplate_0(-1)), to(0, this.drawTemplate_0(0)), to(1, this.drawTemplate_0(1))]);
    this.MIN_WAIT_0 = Time_getInstance().secondsToTicks_za3lpa$(5);
    this.MAX_WAIT_0 = Time_getInstance().secondsToTicks_za3lpa$(45);
  }
  NonFaction$Companion.prototype.prepareOffscreenLocations = function () {
    var tmp$;
    tmp$ = this.OFFSCREEN.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      NonFaction$Companion_getInstance().getOrCreateVectorField_lfj9be$(element);
    }
  };
  NonFaction$Companion.prototype.offscreenCount = function () {
    return this.fields_0.size;
  };
  NonFaction$Companion.prototype.offscreenTotal = function () {
    return this.OFFSCREEN.size;
  };
  NonFaction$Companion.prototype.getOrCreateVectorField_lfj9be$ = function (destination) {
    var tmp$;
    var maybeField = this.fields_0.get_11rb$(destination);
    var tmp$_0 = maybeField != null;
    if (tmp$_0) {
      tmp$_0 = !maybeField.isEmpty();
    }
    if (tmp$_0) {
      tmp$ = maybeField;
    }
     else {
      var newField = PathUtil_getInstance().calculateVectorField_3e8r0f$(PathUtil_getInstance().generateHeatMap_lfj9be$(destination), destination);
      Loading$Companion_getInstance().draw();
      SoundUtil_getInstance().playOffScreenLocationCreationSound();
      VectorFields_getInstance().draw_v4iyov$(newField);
      this.fields_0.put_xwzc9p$(destination, newField);
      tmp$ = newField;
    }
    return tmp$;
  };
  function NonFaction$Companion$findFarPortal$lambda(closure$pos) {
    return function (it) {
      return closure$pos.distanceTo_lfj9be$(it.location);
    };
  }
  var compareByDescending$lambda_1 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(b), selector(a));
      };
    };
  });
  function Comparator$ObjectLiteral_8(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_8.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_8.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  NonFaction$Companion.prototype.findFarPortal_0 = function (pos) {
    return first(sortedWith(World_getInstance().allPortals, new Comparator$ObjectLiteral_8(compareByDescending$lambda_1(NonFaction$Companion$findFarPortal$lambda(pos)))));
  };
  NonFaction$Companion.prototype.createWaitTime_0 = function () {
    return Util_getInstance().randomInt_vux9f0$(this.MIN_WAIT_0, this.MAX_WAIT_0);
  };
  NonFaction$Companion.prototype.image_0 = function (size) {
    var tmp$;
    return (tmp$ = this.images_0.get_11rb$(size.offset)) != null ? tmp$ : this.drawTemplate_0(0);
  };
  function NonFaction$Companion$drawTemplate$lambda(closure$r, closure$lineWidth) {
    return function (ctx) {
      var fillStyle = Colors_getInstance().npcColor;
      var strokeStyle = Colors_getInstance().black;
      var circle = new Circle(Coords_init(closure$r + closure$lineWidth | 0, closure$r + closure$lineWidth | 0), closure$r);
      DrawUtil_getInstance().drawCircle_3kie0f$(ctx, circle, strokeStyle, closure$lineWidth, fillStyle);
    };
  }
  NonFaction$Companion.prototype.drawTemplate_0 = function (sizeOffset) {
    var lineWidth = 2;
    var r = Dim_getInstance().agentRadius + sizeOffset | 0;
    var w = (r * 2 | 0) + (2 * lineWidth | 0) | 0;
    var h = w;
    return HtmlUtil_getInstance().preRender_yb5akz$(w, h, NonFaction$Companion$drawTemplate$lambda(r, lineWidth));
  };
  NonFaction$Companion.prototype.findNearestTo_lfj9be$ = function (pos) {
    var tmp$;
    var $receiver = World_getInstance().allNonFaction;
    var minBy$result;
    minBy$break: do {
      var iterator = $receiver.iterator();
      if (!iterator.hasNext()) {
        minBy$result = null;
        break minBy$break;
      }
      var minElem = iterator.next();
      var minValue = minElem.pos.distanceTo_lfj9be$(pos);
      while (iterator.hasNext()) {
        var e = iterator.next();
        var v = e.pos.distanceTo_lfj9be$(pos);
        if (Kotlin.compareTo(minValue, v) > 0) {
          minElem = e;
          minValue = v;
        }
      }
      minBy$result = minElem;
    }
     while (false);
    tmp$ = minBy$result;
    if (tmp$ == null) {
      throw IllegalStateException_init('Unable to find nearest to ' + pos);
    }
    return tmp$;
  };
  NonFaction$Companion.prototype.create_5edep5$ = function (grid) {
    var tmp$;
    var position = Coords$Companion_getInstance().createRandomPassable_5edep5$(grid);
    var size = AgentSize$Companion_getInstance().createRandom();
    var speed = Skills$Companion_getInstance().randomNpcSpeed();
    if (Util_getInstance().random() < 0.1) {
      var destination = first(Util_getInstance().shuffle_bemo1h$(this.OFFSCREEN));
      var vectorField = this.getOrCreateVectorField_lfj9be$(destination);
      tmp$ = new NonFaction(position, speed, size, destination, vectorField, World_getInstance().tick);
    }
     else {
      var portal = World_getInstance().allPortals.get_za3lpa$(numberToInt(Util_getInstance().random() * (World_getInstance().allPortals.size - 1 | 0)));
      tmp$ = new NonFaction(position, speed, size, portal.location, portal.vectorField, World_getInstance().tick);
    }
    var newNonFaction = tmp$;
    SoundUtil_getInstance().playNpcCreationSound_3mzr9k$(newNonFaction);
    DrawUtil_getInstance().drawNonFaction_3mzr9k$(newNonFaction);
    return newNonFaction;
  };
  NonFaction$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var NonFaction$Companion_instance = null;
  function NonFaction$Companion_getInstance() {
    if (NonFaction$Companion_instance === null) {
      new NonFaction$Companion();
    }
    return NonFaction$Companion_instance;
  }
  NonFaction.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'NonFaction',
    interfaces: []
  };
  NonFaction.prototype.component1 = function () {
    return this.pos;
  };
  NonFaction.prototype.component2 = function () {
    return this.speed;
  };
  NonFaction.prototype.component3 = function () {
    return this.size;
  };
  NonFaction.prototype.component4 = function () {
    return this.destination;
  };
  NonFaction.prototype.component5 = function () {
    return this.vectorField;
  };
  NonFaction.prototype.component6 = function () {
    return this.busyUntil;
  };
  NonFaction.prototype.copy_v4627k$ = function (pos, speed, size, destination, vectorField, busyUntil) {
    return new NonFaction(pos === void 0 ? this.pos : pos, speed === void 0 ? this.speed : speed, size === void 0 ? this.size : size, destination === void 0 ? this.destination : destination, vectorField === void 0 ? this.vectorField : vectorField, busyUntil === void 0 ? this.busyUntil : busyUntil);
  };
  NonFaction.prototype.toString = function () {
    return 'NonFaction(pos=' + Kotlin.toString(this.pos) + (', speed=' + Kotlin.toString(this.speed)) + (', size=' + Kotlin.toString(this.size)) + (', destination=' + Kotlin.toString(this.destination)) + (', vectorField=' + Kotlin.toString(this.vectorField)) + (', busyUntil=' + Kotlin.toString(this.busyUntil)) + ')';
  };
  NonFaction.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.pos) | 0;
    result = result * 31 + Kotlin.hashCode(this.speed) | 0;
    result = result * 31 + Kotlin.hashCode(this.size) | 0;
    result = result * 31 + Kotlin.hashCode(this.destination) | 0;
    result = result * 31 + Kotlin.hashCode(this.vectorField) | 0;
    result = result * 31 + Kotlin.hashCode(this.busyUntil) | 0;
    return result;
  };
  NonFaction.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.pos, other.pos) && Kotlin.equals(this.speed, other.speed) && Kotlin.equals(this.size, other.size) && Kotlin.equals(this.destination, other.destination) && Kotlin.equals(this.vectorField, other.vectorField) && Kotlin.equals(this.busyUntil, other.busyUntil)))));
  };
  function QActions() {
    QActions_instance = this;
    this.MOVE_ELSEWHERE = new QValue('move', 0.01, 'move elsewhere', ActionItem$Companion_getInstance().getIcon_5bvev3$(ActionItem$Companion_getInstance().MOVE));
    this.RECRUIT = new QValue('recruit', 5.0E-4, 'recruit agents', ActionItem$Companion_getInstance().getIcon_5bvev3$(ActionItem$Companion_getInstance().RECRUIT));
    this.EXPLORE = new QValue('explore', 2.0E-4, 'explore portals', ActionItem$Companion_getInstance().getIcon_5bvev3$(ActionItem$Companion_getInstance().EXPLORE));
    this.RECYCLE = new QValue('recycle', 1.0, 'recycle items', ActionItem$Companion_getInstance().getIcon_5bvev3$(ActionItem$Companion_getInstance().RECYCLE));
    this.RECHARGE = new QValue('recharge', 1.0, 'recharge portals', ActionItem$Companion_getInstance().getIcon_5bvev3$(ActionItem$Companion_getInstance().RECHARGE));
    this.HACK = new QValue('hack', 1.0, 'hack portal', ActionItem$Companion_getInstance().getIcon_5bvev3$(ActionItem$Companion_getInstance().HACK));
    this.GLYPH = new QValue('glyph', 1.0, 'glyph portal', ActionItem$Companion_getInstance().getIcon_5bvev3$(ActionItem$Companion_getInstance().GLYPH));
    this.DEPLOY = new QValue('deploy', 1.0, 'deploy portal', ActionItem$Companion_getInstance().getIcon_5bvev3$(ActionItem$Companion_getInstance().DEPLOY));
    this.CAPTURE = new QValue('capture', 1.0, 'capture portal', ActionItem$Companion_getInstance().getIcon_5bvev3$(ActionItem$Companion_getInstance().CAPTURE));
    this.LINK = new QValue('link', 1.0, 'create link', ActionItem$Companion_getInstance().getIcon_5bvev3$(ActionItem$Companion_getInstance().LINK));
    this.ATTACK = new QValue('attack', 1.0, 'attack portals', ActionItem$Companion_getInstance().getIcon_5bvev3$(ActionItem$Companion_getInstance().ATTACK));
  }
  QActions.prototype.values = function () {
    return listOf([this.MOVE_ELSEWHERE, this.EXPLORE, this.RECRUIT, this.ATTACK, this.LINK, this.DEPLOY, this.CAPTURE, this.HACK, this.GLYPH, this.RECHARGE, this.RECYCLE]);
  };
  QActions.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'QActions',
    interfaces: []
  };
  var QActions_instance = null;
  function QActions_getInstance() {
    if (QActions_instance === null) {
      new QActions();
    }
    return QActions_instance;
  }
  function QDestinations() {
    QDestinations_instance = this;
    this.MOVE_TO_RANDOM = new QValue('toRandom', 0.1, 'random');
    this.MOVE_TO_NEAR = new QValue('toNear', 0.2, 'near');
    this.MOVE_TO_UNCAPTURED = new QValue('toUncaptured', 1.0, 'uncaptured');
    this.MOVE_TO_MOST_FRIENDLY = new QValue('toMostFriendly', 0.8, 'most friendly');
    this.MOVE_TO_NEAR_ENEMY = new QValue('toNearEnemy', 0.05, 'near enemy');
    this.MOVE_TO_WEAK_ENEMY = new QValue('toWeakEnemy', 0.02, 'weak enemy');
    this.MOVE_TO_STRONG_ENEMY = new QValue('toStrongEnemy', 0.02, 'strong enemy');
  }
  QDestinations.prototype.values = function () {
    return listOf([this.MOVE_TO_RANDOM, this.MOVE_TO_NEAR, this.MOVE_TO_UNCAPTURED, this.MOVE_TO_MOST_FRIENDLY, this.MOVE_TO_NEAR_ENEMY, this.MOVE_TO_WEAK_ENEMY, this.MOVE_TO_STRONG_ENEMY]);
  };
  QDestinations.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'QDestinations',
    interfaces: []
  };
  var QDestinations_instance = null;
  function QDestinations_getInstance() {
    if (QDestinations_instance === null) {
      new QDestinations();
    }
    return QDestinations_instance;
  }
  function QValue(id, weight, description, icon) {
    if (icon === void 0)
      icon = null;
    this.id = id;
    this.weight = weight;
    this.description = description;
    this.icon = icon;
    this.sliderId = this.id + 'Slider';
  }
  QValue.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'QValue',
    interfaces: []
  };
  function Skills(speed, deployPrecision, glyphSkill, reliability) {
    Skills$Companion_getInstance();
    if (glyphSkill === void 0)
      glyphSkill = 0.8;
    this.speed = speed;
    this.deployPrecision = deployPrecision;
    this.glyphSkill = glyphSkill;
    this.reliability = reliability;
  }
  Skills.prototype.inRangeSpeed = function () {
    return this.speed / Constants_getInstance().phi;
  };
  function Skills$Companion() {
    Skills$Companion_instance = this;
    this.minSpeed_0 = 1.5;
    this.maxSpeed_0 = this.minSpeed_0 * Constants_getInstance().phi;
  }
  Skills$Companion.prototype.createRandom = function () {
    return new Skills(this.randomSpeed_0(), this.deployPrecision_0(), this.randomGlyphSkill_0(), this.randomReliability_0());
  };
  Skills$Companion.prototype.randomSpeed_0 = function () {
    return Util_getInstance().random() * (this.maxSpeed_0 - this.minSpeed_0) + this.minSpeed_0;
  };
  Skills$Companion.prototype.deployPrecision_0 = function () {
    return 0.7 + Util_getInstance().random() * 0.3;
  };
  Skills$Companion.prototype.randomGlyphSkill_0 = function () {
    return 0.5 + Util_getInstance().random() * 0.5;
  };
  Skills$Companion.prototype.randomReliability_0 = function () {
    return 0.5 + Util_getInstance().random() / 2.0;
  };
  Skills$Companion.prototype.randomNpcSpeed = function () {
    return this.randomSpeed_0() / Constants_getInstance().phi;
  };
  Skills$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Skills$Companion_instance = null;
  function Skills$Companion_getInstance() {
    if (Skills$Companion_instance === null) {
      new Skills$Companion();
    }
    return Skills$Companion_instance;
  }
  Skills.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Skills',
    interfaces: []
  };
  Skills.prototype.component1 = function () {
    return this.speed;
  };
  Skills.prototype.component2 = function () {
    return this.deployPrecision;
  };
  Skills.prototype.component3 = function () {
    return this.glyphSkill;
  };
  Skills.prototype.component4 = function () {
    return this.reliability;
  };
  Skills.prototype.copy_6y0v78$ = function (speed, deployPrecision, glyphSkill, reliability) {
    return new Skills(speed === void 0 ? this.speed : speed, deployPrecision === void 0 ? this.deployPrecision : deployPrecision, glyphSkill === void 0 ? this.glyphSkill : glyphSkill, reliability === void 0 ? this.reliability : reliability);
  };
  Skills.prototype.toString = function () {
    return 'Skills(speed=' + Kotlin.toString(this.speed) + (', deployPrecision=' + Kotlin.toString(this.deployPrecision)) + (', glyphSkill=' + Kotlin.toString(this.glyphSkill)) + (', reliability=' + Kotlin.toString(this.reliability)) + ')';
  };
  Skills.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.speed) | 0;
    result = result * 31 + Kotlin.hashCode(this.deployPrecision) | 0;
    result = result * 31 + Kotlin.hashCode(this.glyphSkill) | 0;
    result = result * 31 + Kotlin.hashCode(this.reliability) | 0;
    return result;
  };
  Skills.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.speed, other.speed) && Kotlin.equals(this.deployPrecision, other.deployPrecision) && Kotlin.equals(this.glyphSkill, other.glyphSkill) && Kotlin.equals(this.reliability, other.reliability)))));
  };
  function Colors() {
    Colors_instance = this;
    this.transparent = '#00000000';
    this.background = '000000';
    this.nextPortal = '#aaaaaa';
    this.destination = '#333333';
    this.agentDeployCircle = '#aaaaaa';
    this.grid = '#111111';
    this.enl = '#03DC03';
    this.res = '#0088FF';
    this.black = '#000000';
    this.white = '#ffffff';
    this.red = '#ff0000';
    this.orange = '#ff7315';
    this.damage = '#ff7315';
    this.critDamage = '#e40000';
    this.npcColor = '#dddddd';
  }
  Colors.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Colors',
    interfaces: []
  };
  var Colors_instance = null;
  function Colors_getInstance() {
    if (Colors_instance === null) {
      new Colors();
    }
    return Colors_instance;
  }
  function Config() {
    Config_instance = this;
    this.minPortals = 3;
    this.maxPortals = 89;
    this.minFrogs = 2;
    this.maxFrogs = 21;
    this.minSmurfs = 2;
    this.maxSmurfs = 21;
    this.frogQuitRate = 0.1;
    this.smurfQuitRate = 0.1;
    this.factionChangeRate = 0.01;
    this.portalRemovalRate = 0.1;
    this.startPortals = 5;
    this.maxNonFaction_0 = 300;
    this.apMultiplier = 10;
    this.isNpcSwarming = true;
    this.npcXmSpawnRatio = 0.2;
    this.isSoundOn = !HtmlUtil_getInstance().isLocal();
    this.isPlayInitialSound = false;
    this.isSatOn = false;
    this.isHighlighActionLimit = true;
    this.vectorSmoothCount = 3;
    this.shadowBlurCount = 3;
    this.comMessageLimit = 8;
    this.topAgentsMessageLimit = 8;
    this.ticksPerCheckpoint = Time_getInstance().secondsToTicks_za3lpa$(300);
    this.ticksPerCycle = Time_getInstance().secondsToTicks_za3lpa$(1800);
    this.pathResolution = 10;
    this.useOffscreenEdgeDestinations = false;
  }
  Config.prototype.startFrogs = function () {
    return HtmlUtil_getInstance().isQuickstart() ? 8 : 2;
  };
  Config.prototype.startSmurfs = function () {
    return HtmlUtil_getInstance().isQuickstart() ? 8 : 2;
  };
  Config.prototype.initialAp = function () {
    return HtmlUtil_getInstance().isQuickstart() ? 2000000 : 0;
  };
  Config.prototype.maxFor_bip15f$ = function (faction) {
    switch (faction.name) {
      case 'ENL':
        return 21;
      case 'RES':
        return 21;
      default:return 300;
    }
  };
  Config.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Config',
    interfaces: []
  };
  var Config_instance = null;
  function Config_getInstance() {
    if (Config_instance === null) {
      new Config();
    }
    return Config_instance;
  }
  function Constants() {
    Constants_instance = this;
    this.phi = 1.618033988749895;
    this.tau = 2.0 * math.PI;
    this.hexChars = '0123456789ABCDEF';
    this.localLocation_0 = 'http://localhost:63342/';
    this.localToken_0 = 'Qgress/';
    this.location_0 = 'https://tok.github.io/';
    this.token_0 = 'Q-gress/';
  }
  Constants.prototype.token = function () {
    return HtmlUtil_getInstance().isLocal() ? this.localToken_0 : this.token_0;
  };
  Constants.prototype.targetUrl = function () {
    return HtmlUtil_getInstance().isLocal() ? this.localLocation_0 : this.location_0;
  };
  Constants.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Constants',
    interfaces: []
  };
  var Constants_instance = null;
  function Constants_getInstance() {
    if (Constants_instance === null) {
      new Constants();
    }
    return Constants_instance;
  }
  function Dim() {
    Dim_instance = this;
    this.width = 1200;
    this.height = 800;
    this.portalRadius = 8.0;
    this.portalLineWidth = 2;
    this.minDistanceBetweenPortals = 2 * this.portalRadius * 3;
    this.minDistancePortalToImpassable = this.portalRadius;
    this.resoRadius = 2.0;
    this.maxDeploymentRange = 34.0;
    this.minDeploymentRange = 13.0;
    this.agentRadius = 3;
    this.agentLineWidth = 2;
    this.agentDeployCircleLineWidth = 1.0;
    this.linkLineWidth = 3.0;
    this.botActionOffset = 160.0;
    this.portalXmSpawnRadius = 40;
    this.npcXmSpawnRadius = 10;
    this.agentXmCollectionRadius = this.maxDeploymentRange;
    this.leftOffset = numberToInt(this.maxDeploymentRange) * Constants_getInstance().phi;
    this.rightOffset = numberToInt(this.maxDeploymentRange) * Constants_getInstance().phi;
    this.topOffset = numberToInt(this.maxDeploymentRange) * Constants_getInstance().phi;
    this.botOffset = numberToInt(this.maxDeploymentRange) * Constants_getInstance().phi;
    this.comBottomOffset = 34;
    this.comRightOffset = 340;
    this.comFontSize = 11;
    this.muFontSize = 21;
    this.muLeftOffset = 13;
    this.muBottomOffset = 89;
    this.pixelToMFactor = 0.5;
    this.statsTopOffset = 13;
    this.statsRightOffset = 170;
    this.loadingBarLength = 377.0;
    this.loadingFontSize = 21;
    this.cycleRightOffset = 510;
    this.cycleTopOffset = 3;
    this.cycleH = 86;
    this.topAgentsBottomOffset = 0;
    this.topAgentsLeftOffset = 210;
    this.topAgentsFontSize = 11;
    this.topAgentsInventoryFontSize = 11;
    this.tickBottomOffset = 55;
    this.tickFontSize = 12;
    this.portalNameFontSize = 12;
  }
  Dim.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Dim',
    interfaces: []
  };
  var Dim_instance = null;
  function Dim_getInstance() {
    if (Dim_instance === null) {
      new Dim();
    }
    return Dim_instance;
  }
  function Location(name, ordinal, displayName, lng, lat) {
    Enum.call(this);
    this.displayName = displayName;
    this.lng = lng;
    this.lat = lat;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function Location_initFields() {
    Location_initFields = function () {
    };
    Location$RED_SQUARE_instance = new Location('RED_SQUARE', 0, 'Red Square St. Gallen', 9.37327, 47.42214);
    Location$RED_SQUARE_MOSCOW_instance = new Location('RED_SQUARE_MOSCOW', 1, 'Red Square Moscow', 37.6205, 55.754);
    Location$CHLOSER_PLATZ_instance = new Location('CHLOSER_PLATZ', 2, 'Chloster Platz St. Gallen', 9.377, 47.424);
    Location$GOLLUMS_instance = new Location('GOLLUMS', 3, 'Gollums', 8.5952, 47.362);
    Location$BAD_RAGAZ_instance = new Location('BAD_RAGAZ', 4, 'Bad Ragaz', 9.50032, 47.00247);
    Location$ESCHER_WYSS_instance = new Location('ESCHER_WYSS', 5, 'Escher Wyss', 8.52206, 47.3908);
    Location$GIZA_PLATEAU_instance = new Location('GIZA_PLATEAU', 6, 'Giza Plateau', 31.132, 29.978);
    Location$EIFFEL_TOWER_instance = new Location('EIFFEL_TOWER', 7, 'Eiffel Tower', 2.29486, 48.85824);
    Location$PRIME_TOWER_instance = new Location('PRIME_TOWER', 8, 'Prime Tower', 8.51831, 47.38673);
    Location$GROUND_ZERO_instance = new Location('GROUND_ZERO', 9, 'Ground Zero', -74.0123, 40.7125);
    Location$PLATZSPITZ_instance = new Location('PLATZSPITZ', 10, 'Platzspitz', 8.539, 47.3821);
    Location$Companion_getInstance();
  }
  var Location$RED_SQUARE_instance;
  function Location$RED_SQUARE_getInstance() {
    Location_initFields();
    return Location$RED_SQUARE_instance;
  }
  var Location$RED_SQUARE_MOSCOW_instance;
  function Location$RED_SQUARE_MOSCOW_getInstance() {
    Location_initFields();
    return Location$RED_SQUARE_MOSCOW_instance;
  }
  var Location$CHLOSER_PLATZ_instance;
  function Location$CHLOSER_PLATZ_getInstance() {
    Location_initFields();
    return Location$CHLOSER_PLATZ_instance;
  }
  var Location$GOLLUMS_instance;
  function Location$GOLLUMS_getInstance() {
    Location_initFields();
    return Location$GOLLUMS_instance;
  }
  var Location$BAD_RAGAZ_instance;
  function Location$BAD_RAGAZ_getInstance() {
    Location_initFields();
    return Location$BAD_RAGAZ_instance;
  }
  var Location$ESCHER_WYSS_instance;
  function Location$ESCHER_WYSS_getInstance() {
    Location_initFields();
    return Location$ESCHER_WYSS_instance;
  }
  var Location$GIZA_PLATEAU_instance;
  function Location$GIZA_PLATEAU_getInstance() {
    Location_initFields();
    return Location$GIZA_PLATEAU_instance;
  }
  var Location$EIFFEL_TOWER_instance;
  function Location$EIFFEL_TOWER_getInstance() {
    Location_initFields();
    return Location$EIFFEL_TOWER_instance;
  }
  var Location$PRIME_TOWER_instance;
  function Location$PRIME_TOWER_getInstance() {
    Location_initFields();
    return Location$PRIME_TOWER_instance;
  }
  var Location$GROUND_ZERO_instance;
  function Location$GROUND_ZERO_getInstance() {
    Location_initFields();
    return Location$GROUND_ZERO_instance;
  }
  var Location$PLATZSPITZ_instance;
  function Location$PLATZSPITZ_getInstance() {
    Location_initFields();
    return Location$PLATZSPITZ_instance;
  }
  Location.prototype.toJSONString = function () {
    return '[' + this.lng + ',' + this.lat + ']';
  };
  Location.prototype.toJSON = function () {
    return JSON.parse(this.toJSONString());
  };
  function Location$Companion() {
    Location$Companion_instance = this;
    this.DEFAULT = Location$RED_SQUARE_getInstance();
  }
  Location$Companion.prototype.random = function () {
    return Util_getInstance().shuffle_bemo1h$(asList(Location$values())).get_za3lpa$(0);
  };
  Location$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Location$Companion_instance = null;
  function Location$Companion_getInstance() {
    Location_initFields();
    if (Location$Companion_instance === null) {
      new Location$Companion();
    }
    return Location$Companion_instance;
  }
  Location.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Location',
    interfaces: [Enum]
  };
  function Location$values() {
    return [Location$RED_SQUARE_getInstance(), Location$RED_SQUARE_MOSCOW_getInstance(), Location$CHLOSER_PLATZ_getInstance(), Location$GOLLUMS_getInstance(), Location$BAD_RAGAZ_getInstance(), Location$ESCHER_WYSS_getInstance(), Location$GIZA_PLATEAU_getInstance(), Location$EIFFEL_TOWER_getInstance(), Location$PRIME_TOWER_getInstance(), Location$GROUND_ZERO_getInstance(), Location$PLATZSPITZ_getInstance()];
  }
  Location.values = Location$values;
  function Location$valueOf(name) {
    switch (name) {
      case 'RED_SQUARE':
        return Location$RED_SQUARE_getInstance();
      case 'RED_SQUARE_MOSCOW':
        return Location$RED_SQUARE_MOSCOW_getInstance();
      case 'CHLOSER_PLATZ':
        return Location$CHLOSER_PLATZ_getInstance();
      case 'GOLLUMS':
        return Location$GOLLUMS_getInstance();
      case 'BAD_RAGAZ':
        return Location$BAD_RAGAZ_getInstance();
      case 'ESCHER_WYSS':
        return Location$ESCHER_WYSS_getInstance();
      case 'GIZA_PLATEAU':
        return Location$GIZA_PLATEAU_getInstance();
      case 'EIFFEL_TOWER':
        return Location$EIFFEL_TOWER_getInstance();
      case 'PRIME_TOWER':
        return Location$PRIME_TOWER_getInstance();
      case 'GROUND_ZERO':
        return Location$GROUND_ZERO_getInstance();
      case 'PLATZSPITZ':
        return Location$PLATZSPITZ_getInstance();
      default:throwISE('No enum constant config.Location.' + name);
    }
  }
  Location.valueOf_61zpoe$ = Location$valueOf;
  function OscillatorType() {
    OscillatorType_instance = this;
    this.SINE = 'sine';
    this.SQUARE = 'square';
    this.SAW = 'sawtooth';
    this.TRIANGLE = 'triangle';
    this.CUSTOM = 'custom';
  }
  OscillatorType.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'OscillatorType',
    interfaces: []
  };
  var OscillatorType_instance = null;
  function OscillatorType_getInstance() {
    if (OscillatorType_instance === null) {
      new OscillatorType();
    }
    return OscillatorType_instance;
  }
  function Probabilities() {
    Probabilities_instance = this;
    this.keyChance = 0.8;
    this.hackChance = 0.75;
    this.unclaimedHackChance = 0.85;
    this.glyphChance = 0.5;
    this.unclaimedGlyphChance = 0.6;
  }
  Probabilities.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Probabilities',
    interfaces: []
  };
  var Probabilities_instance = null;
  function Probabilities_getInstance() {
    if (Probabilities_instance === null) {
      new Probabilities();
    }
    return Probabilities_instance;
  }
  function Styles() {
    Styles_instance = this;
    this.fieldTransparency = 0.4;
    this.isDrawAgentRange = false;
    this.isDrawDestination = false;
    this.isDrawNoiseMap = false;
    this.isDrawPortalNames = true;
    this.isDrawCom = true;
    this.isDrawResoLevels = false;
    this.isDrawTopAgents = true;
    this.use3DBuildings = true;
    this.isDrawObstructedVectors = false;
    this.isDrawResoLineGradient = true;
    this.isFillMuDisplay = true;
  }
  Styles.prototype.vectorStyle = function () {
    return HtmlUtil_getInstance().isShowSatelliteMap() ? VectorStyle$SQUARE_getInstance() : VectorStyle$CIRCLE_getInstance();
  };
  Styles.prototype.isColorVectors = function () {
    return !HtmlUtil_getInstance().isShowSatelliteMap();
  };
  Styles.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Styles',
    interfaces: []
  };
  var Styles_instance = null;
  function Styles_getInstance() {
    if (Styles_instance === null) {
      new Styles();
    }
    return Styles_instance;
  }
  function Time() {
    Time_instance = this;
    this.minTickInterval = 20;
    this.secondsPerTick_0 = 1;
  }
  Time.prototype.ticksToSeconds_za3lpa$ = function (ticks) {
    return ticks * 1 | 0;
  };
  Time.prototype.secondsToTicks_za3lpa$ = function (seconds) {
    return seconds / 1 | 0;
  };
  Time.prototype.ticksToTimestamp_za3lpa$ = function (ticks) {
    return Util_getInstance().formatSeconds_za3lpa$(this.ticksToSeconds_za3lpa$(ticks));
  };
  Time.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Time',
    interfaces: []
  };
  var Time_instance = null;
  function Time_getInstance() {
    if (Time_instance === null) {
      new Time();
    }
    return Time_instance;
  }
  function VectorStyle(name, ordinal) {
    Enum.call(this);
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function VectorStyle_initFields() {
    VectorStyle_initFields = function () {
    };
    VectorStyle$CIRCLE_instance = new VectorStyle('CIRCLE', 0);
    VectorStyle$SQUARE_instance = new VectorStyle('SQUARE', 1);
  }
  var VectorStyle$CIRCLE_instance;
  function VectorStyle$CIRCLE_getInstance() {
    VectorStyle_initFields();
    return VectorStyle$CIRCLE_instance;
  }
  var VectorStyle$SQUARE_instance;
  function VectorStyle$SQUARE_getInstance() {
    VectorStyle_initFields();
    return VectorStyle$SQUARE_instance;
  }
  VectorStyle.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'VectorStyle',
    interfaces: [Enum]
  };
  function VectorStyle$values() {
    return [VectorStyle$CIRCLE_getInstance(), VectorStyle$SQUARE_getInstance()];
  }
  VectorStyle.values = VectorStyle$values;
  function VectorStyle$valueOf(name) {
    switch (name) {
      case 'CIRCLE':
        return VectorStyle$CIRCLE_getInstance();
      case 'SQUARE':
        return VectorStyle$SQUARE_getInstance();
      default:throwISE('No enum constant config.VectorStyle.' + name);
    }
  }
  VectorStyle.valueOf_61zpoe$ = VectorStyle$valueOf;
  function DeployableItem() {
  }
  DeployableItem.$metadata$ = {
    kind: Kind_INTERFACE,
    simpleName: 'DeployableItem',
    interfaces: [QgressItem]
  };
  function LinkAmp(type, slot, owner) {
    LinkAmp$Companion_getInstance();
    this.type = type;
    this.slot = slot;
    this.owner = owner;
  }
  LinkAmp.prototype.isDeployed = function () {
    return this.slot != null;
  };
  LinkAmp.prototype.deploy_hv9zn6$ = function (portal) {
    console.info('Deploying ' + this + ' to portal ' + portal);
  };
  LinkAmp.prototype.toString = function () {
    return this.type.abbr;
  };
  LinkAmp.prototype.getOwnerId = function () {
    return this.owner.key();
  };
  LinkAmp.prototype.getLevel = function () {
    return -1;
  };
  function LinkAmp$Companion() {
    LinkAmp$Companion_instance = this;
  }
  LinkAmp$Companion.prototype.calculateImprovedRange_awbt68$ = function (allModsInPortal, range) {
    var tmp$;
    var destination = ArrayList_init_0();
    var tmp$_0;
    tmp$_0 = allModsInPortal.iterator();
    while (tmp$_0.hasNext()) {
      var element = tmp$_0.next();
      if (Kotlin.isType(element, LinkAmp))
        destination.add_11rb$(element);
    }
    var linkamps = destination;
    switch (linkamps.size) {
      case 1:
        tmp$ = range * 2;
        break;
      case 2:
        tmp$ = range * 2.5;
        break;
      case 3:
        tmp$ = range * 2.75;
        break;
      case 4:
        tmp$ = range * 5;
        break;
      default:tmp$ = range;
        break;
    }
    return tmp$;
  };
  LinkAmp$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var LinkAmp$Companion_instance = null;
  function LinkAmp$Companion_getInstance() {
    if (LinkAmp$Companion_instance === null) {
      new LinkAmp$Companion();
    }
    return LinkAmp$Companion_instance;
  }
  LinkAmp.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'LinkAmp',
    interfaces: [DeployableItem]
  };
  LinkAmp.prototype.component1 = function () {
    return this.type;
  };
  LinkAmp.prototype.component2 = function () {
    return this.slot;
  };
  LinkAmp.prototype.component3 = function () {
    return this.owner;
  };
  LinkAmp.prototype.copy_e02dju$ = function (type, slot, owner) {
    return new LinkAmp(type === void 0 ? this.type : type, slot === void 0 ? this.slot : slot, owner === void 0 ? this.owner : owner);
  };
  LinkAmp.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.type) | 0;
    result = result * 31 + Kotlin.hashCode(this.slot) | 0;
    result = result * 31 + Kotlin.hashCode(this.owner) | 0;
    return result;
  };
  LinkAmp.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.type, other.type) && Kotlin.equals(this.slot, other.slot) && Kotlin.equals(this.owner, other.owner)))));
  };
  function Multihack(type, slot, owner) {
    Multihack$Companion_getInstance();
    this.type = type;
    this.slot = slot;
    this.owner = owner;
  }
  Multihack.prototype.isDeployed = function () {
    return this.slot != null;
  };
  Multihack.prototype.deploy_hv9zn6$ = function (portal) {
    console.info('Deploying ' + this + ' to portal ' + portal);
  };
  Multihack.prototype.toString = function () {
    return this.type.abbr;
  };
  Multihack.prototype.getOwnerId = function () {
    return this.owner.key();
  };
  Multihack.prototype.getLevel = function () {
    return -1;
  };
  function Multihack$Companion() {
    Multihack$Companion_instance = this;
  }
  function Multihack$Companion$calculateImprovedBurnout$lambda(it) {
    return it.type.order;
  }
  var compareBy$lambda_6 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_9(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_9.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_9.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Multihack$Companion.prototype.calculateImprovedBurnout_6l8466$ = function (allModsInPortal) {
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = allModsInPortal.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (Kotlin.isType(element, Multihack))
        destination.add_11rb$(element);
    }
    var destination_0 = ArrayList_init(collectionSizeOrDefault(destination, 10));
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      var tmp$_1;
      destination_0.add_11rb$(Kotlin.isType(tmp$_1 = item, Multihack) ? tmp$_1 : throwCCE());
    }
    var multihacks = sortedWith(destination_0, new Comparator$ObjectLiteral_9(compareBy$lambda_6(Multihack$Companion$calculateImprovedBurnout$lambda)));
    var first_0 = first(multihacks).type.additionalHacks;
    var second = multihacks.get_za3lpa$(1).type.additionalHacks * 0.5;
    var third = multihacks.get_za3lpa$(2).type.additionalHacks * 0.5;
    var fourth = multihacks.get_za3lpa$(3).type.additionalHacks * 0.5;
    return first_0 + second + third + fourth;
  };
  Multihack$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Multihack$Companion_instance = null;
  function Multihack$Companion_getInstance() {
    if (Multihack$Companion_instance === null) {
      new Multihack$Companion();
    }
    return Multihack$Companion_instance;
  }
  Multihack.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Multihack',
    interfaces: [DeployableItem]
  };
  Multihack.prototype.component1 = function () {
    return this.type;
  };
  Multihack.prototype.component2 = function () {
    return this.slot;
  };
  Multihack.prototype.component3 = function () {
    return this.owner;
  };
  Multihack.prototype.copy_ldg7oq$ = function (type, slot, owner) {
    return new Multihack(type === void 0 ? this.type : type, slot === void 0 ? this.slot : slot, owner === void 0 ? this.owner : owner);
  };
  Multihack.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.type) | 0;
    result = result * 31 + Kotlin.hashCode(this.slot) | 0;
    result = result * 31 + Kotlin.hashCode(this.owner) | 0;
    return result;
  };
  Multihack.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.type, other.type) && Kotlin.equals(this.slot, other.slot) && Kotlin.equals(this.owner, other.owner)))));
  };
  function Resonator(owner, level, energy, portal, octant, coords) {
    Resonator$Companion_getInstance();
    if (portal === void 0)
      portal = null;
    if (octant === void 0)
      octant = null;
    if (coords === void 0)
      coords = null;
    this.owner = owner;
    this.level = level;
    this.energy = energy;
    this.portal = portal;
    this.octant = octant;
    this.coords = coords;
  }
  Resonator.prototype.calcHealthPercent = function () {
    return (this.energy * 100 | 0) / this.level.energy | 0;
  };
  Resonator.prototype.isAtCriticalLevel = function () {
    return this.calcHealthPercent() < 20;
  };
  Resonator.prototype.totalCapacity = function () {
    return this.level.energy;
  };
  Resonator.prototype.openCapacity = function () {
    return this.totalCapacity() - this.energy | 0;
  };
  Resonator.prototype.recharge_2b7tta$ = function (agent, xm) {
    var b = this.openCapacity();
    var value = Math_0.min(xm, b);
    var a = this.energy + value | 0;
    var b_0 = this.totalCapacity();
    this.energy = Math_0.min(a, b_0);
    agent.removeXm_za3lpa$(value);
    agent.addAp_za3lpa$(10);
  };
  Resonator.prototype.decayEnergy_0 = function () {
    return numberToInt(this.level.energy * Resonator$Companion_getInstance().DECAY_RATIO);
  };
  Resonator.prototype.decay = function () {
    var tmp$;
    var a = this.energy - this.decayEnergy_0() | 0;
    var newEnergy = Math_0.max(a, 0);
    this.energy = newEnergy;
    if (newEnergy <= 0) {
      (tmp$ = this.portal) != null ? (tmp$.removeReso_j436sm$(ensureNotNull(this.octant), null), Unit) : null;
    }
  };
  Resonator.prototype.takeDamage_2b7tta$ = function (agent, damage) {
    var tmp$;
    var a = this.energy - damage | 0;
    var newEnergy = Math_0.max(a, 0);
    this.energy = newEnergy;
    if (newEnergy <= newEnergy) {
      agent.addAp_za3lpa$(75);
      (tmp$ = this.portal) != null ? (tmp$.removeReso_j436sm$(ensureNotNull(this.octant), agent), Unit) : null;
    }
  };
  Resonator.prototype.deploy_njiqqf$ = function (portal, octant, coords) {
    this.portal = portal;
    this.octant = octant;
    this.coords = coords;
  };
  Resonator.prototype.toString = function () {
    return 'R' + toString(this.level.level);
  };
  Resonator.prototype.getOwnerId = function () {
    return this.owner.key();
  };
  Resonator.prototype.getLevel = function () {
    return this.level.level;
  };
  function Resonator$Companion() {
    Resonator$Companion_instance = this;
    this.DECAY_RATIO = 0.15;
  }
  Resonator$Companion.prototype.create_yp5k5z$ = function (owner, level) {
    return new Resonator(owner, level, level.energy);
  };
  Resonator$Companion.prototype.create_2b7tta$ = function (owner, level) {
    return this.create_yp5k5z$(owner, ResonatorLevel$Companion_getInstance().valueOf_za3lpa$(level));
  };
  Resonator$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Resonator$Companion_instance = null;
  function Resonator$Companion_getInstance() {
    if (Resonator$Companion_instance === null) {
      new Resonator$Companion();
    }
    return Resonator$Companion_instance;
  }
  Resonator.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Resonator',
    interfaces: [DeployableItem]
  };
  Resonator.prototype.component1 = function () {
    return this.owner;
  };
  Resonator.prototype.component2 = function () {
    return this.level;
  };
  Resonator.prototype.component3 = function () {
    return this.energy;
  };
  Resonator.prototype.component4 = function () {
    return this.portal;
  };
  Resonator.prototype.component5 = function () {
    return this.octant;
  };
  Resonator.prototype.component6 = function () {
    return this.coords;
  };
  Resonator.prototype.copy_xlsrw9$ = function (owner, level, energy, portal, octant, coords) {
    return new Resonator(owner === void 0 ? this.owner : owner, level === void 0 ? this.level : level, energy === void 0 ? this.energy : energy, portal === void 0 ? this.portal : portal, octant === void 0 ? this.octant : octant, coords === void 0 ? this.coords : coords);
  };
  Resonator.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.owner) | 0;
    result = result * 31 + Kotlin.hashCode(this.level) | 0;
    result = result * 31 + Kotlin.hashCode(this.energy) | 0;
    result = result * 31 + Kotlin.hashCode(this.portal) | 0;
    result = result * 31 + Kotlin.hashCode(this.octant) | 0;
    result = result * 31 + Kotlin.hashCode(this.coords) | 0;
    return result;
  };
  Resonator.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.owner, other.owner) && Kotlin.equals(this.level, other.level) && Kotlin.equals(this.energy, other.energy) && Kotlin.equals(this.portal, other.portal) && Kotlin.equals(this.octant, other.octant) && Kotlin.equals(this.coords, other.coords)))));
  };
  function Shield(type, owner) {
    this.type = type;
    this.owner = owner;
  }
  Shield.prototype.toString = function () {
    return this.type.abbr;
  };
  Shield.prototype.getOwnerId = function () {
    return this.owner.key();
  };
  Shield.prototype.getLevel = function () {
    return this.type.level;
  };
  Shield.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Shield',
    interfaces: [DeployableItem]
  };
  Shield.prototype.component1 = function () {
    return this.type;
  };
  Shield.prototype.component2 = function () {
    return this.owner;
  };
  Shield.prototype.copy_nl3jau$ = function (type, owner) {
    return new Shield(type === void 0 ? this.type : type, owner === void 0 ? this.owner : owner);
  };
  Shield.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.type) | 0;
    result = result * 31 + Kotlin.hashCode(this.owner) | 0;
    return result;
  };
  Shield.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.type, other.type) && Kotlin.equals(this.owner, other.owner)))));
  };
  function Virus(type, owner) {
    this.type = type;
    this.owner = owner;
  }
  Virus.prototype.toString = function () {
    return this.type.abbr;
  };
  Virus.prototype.getOwnerId = function () {
    return this.owner.key();
  };
  Virus.prototype.getLevel = function () {
    throw new NotImplementedError('Virus has no level.');
  };
  Virus.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Virus',
    interfaces: [DeployableItem]
  };
  Virus.prototype.component1 = function () {
    return this.type;
  };
  Virus.prototype.component2 = function () {
    return this.owner;
  };
  Virus.prototype.copy_38flno$ = function (type, owner) {
    return new Virus(type === void 0 ? this.type : type, owner === void 0 ? this.owner : owner);
  };
  Virus.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.type) | 0;
    result = result * 31 + Kotlin.hashCode(this.owner) | 0;
    return result;
  };
  Virus.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.type, other.type) && Kotlin.equals(this.owner, other.owner)))));
  };
  function ItemLevel() {
  }
  ItemLevel.$metadata$ = {
    kind: Kind_INTERFACE,
    simpleName: 'ItemLevel',
    interfaces: []
  };
  function LevelColor() {
    LevelColor_instance = this;
    this.map = mapOf([to(1, '#FECE5A'), to(2, '#FFA630'), to(3, '#FF7315'), to(4, '#E40000'), to(5, '#FD2992'), to(6, '#EB26CD'), to(7, '#C124E0'), to(8, '#9627F4')]);
  }
  LevelColor.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'LevelColor',
    interfaces: []
  };
  var LevelColor_instance = null;
  function LevelColor_getInstance() {
    if (LevelColor_instance === null) {
      new LevelColor();
    }
    return LevelColor_instance;
  }
  function PortalLevel(name, ordinal, value, display) {
    Enum.call(this);
    this.value = value;
    this.display = display;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function PortalLevel_initFields() {
    PortalLevel_initFields = function () {
    };
    PortalLevel$ZERO_instance = new PortalLevel('ZERO', 0, 0, ' ');
    PortalLevel$ONE_instance = new PortalLevel('ONE', 1, 1, '1');
    PortalLevel$TWO_instance = new PortalLevel('TWO', 2, 2, '2');
    PortalLevel$THREE_instance = new PortalLevel('THREE', 3, 3, '3');
    PortalLevel$FOUR_instance = new PortalLevel('FOUR', 4, 4, '4');
    PortalLevel$FIVE_instance = new PortalLevel('FIVE', 5, 5, '5');
    PortalLevel$SIX_instance = new PortalLevel('SIX', 6, 6, '6');
    PortalLevel$SEVEN_instance = new PortalLevel('SEVEN', 7, 7, '7');
    PortalLevel$EIGHT_instance = new PortalLevel('EIGHT', 8, 8, '8');
    PortalLevel$Companion_getInstance();
  }
  var PortalLevel$ZERO_instance;
  function PortalLevel$ZERO_getInstance() {
    PortalLevel_initFields();
    return PortalLevel$ZERO_instance;
  }
  var PortalLevel$ONE_instance;
  function PortalLevel$ONE_getInstance() {
    PortalLevel_initFields();
    return PortalLevel$ONE_instance;
  }
  var PortalLevel$TWO_instance;
  function PortalLevel$TWO_getInstance() {
    PortalLevel_initFields();
    return PortalLevel$TWO_instance;
  }
  var PortalLevel$THREE_instance;
  function PortalLevel$THREE_getInstance() {
    PortalLevel_initFields();
    return PortalLevel$THREE_instance;
  }
  var PortalLevel$FOUR_instance;
  function PortalLevel$FOUR_getInstance() {
    PortalLevel_initFields();
    return PortalLevel$FOUR_instance;
  }
  var PortalLevel$FIVE_instance;
  function PortalLevel$FIVE_getInstance() {
    PortalLevel_initFields();
    return PortalLevel$FIVE_instance;
  }
  var PortalLevel$SIX_instance;
  function PortalLevel$SIX_getInstance() {
    PortalLevel_initFields();
    return PortalLevel$SIX_instance;
  }
  var PortalLevel$SEVEN_instance;
  function PortalLevel$SEVEN_getInstance() {
    PortalLevel_initFields();
    return PortalLevel$SEVEN_instance;
  }
  var PortalLevel$EIGHT_instance;
  function PortalLevel$EIGHT_getInstance() {
    PortalLevel_initFields();
    return PortalLevel$EIGHT_instance;
  }
  PortalLevel.prototype.toInt = function () {
    return this.value;
  };
  PortalLevel.prototype.getColor = function () {
    var tmp$;
    return (tmp$ = LevelColor_getInstance().map.get_11rb$(this.value)) != null ? tmp$ : '#FFFFFF';
  };
  function PortalLevel$Companion() {
    PortalLevel$Companion_instance = this;
  }
  PortalLevel$Companion.prototype.findByValue_za3lpa$ = function (value) {
    var $receiver = PortalLevel$values();
    var firstOrNull$result;
    firstOrNull$break: do {
      var tmp$;
      for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
        var element = $receiver[tmp$];
        if (element.value === value) {
          firstOrNull$result = element;
          break firstOrNull$break;
        }
      }
      firstOrNull$result = null;
    }
     while (false);
    return ensureNotNull(firstOrNull$result);
  };
  PortalLevel$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var PortalLevel$Companion_instance = null;
  function PortalLevel$Companion_getInstance() {
    PortalLevel_initFields();
    if (PortalLevel$Companion_instance === null) {
      new PortalLevel$Companion();
    }
    return PortalLevel$Companion_instance;
  }
  PortalLevel.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'PortalLevel',
    interfaces: [Enum]
  };
  function PortalLevel$values() {
    return [PortalLevel$ZERO_getInstance(), PortalLevel$ONE_getInstance(), PortalLevel$TWO_getInstance(), PortalLevel$THREE_getInstance(), PortalLevel$FOUR_getInstance(), PortalLevel$FIVE_getInstance(), PortalLevel$SIX_getInstance(), PortalLevel$SEVEN_getInstance(), PortalLevel$EIGHT_getInstance()];
  }
  PortalLevel.values = PortalLevel$values;
  function PortalLevel$valueOf(name) {
    switch (name) {
      case 'ZERO':
        return PortalLevel$ZERO_getInstance();
      case 'ONE':
        return PortalLevel$ONE_getInstance();
      case 'TWO':
        return PortalLevel$TWO_getInstance();
      case 'THREE':
        return PortalLevel$THREE_getInstance();
      case 'FOUR':
        return PortalLevel$FOUR_getInstance();
      case 'FIVE':
        return PortalLevel$FIVE_getInstance();
      case 'SIX':
        return PortalLevel$SIX_getInstance();
      case 'SEVEN':
        return PortalLevel$SEVEN_getInstance();
      case 'EIGHT':
        return PortalLevel$EIGHT_getInstance();
      default:throwISE('No enum constant items.level.PortalLevel.' + name);
    }
  }
  PortalLevel.valueOf_61zpoe$ = PortalLevel$valueOf;
  function PowerCubeLevel(name, ordinal, level, xmValue) {
    Enum.call(this);
    this.level = level;
    this.xmValue_5hxmsq$_0 = xmValue;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function PowerCubeLevel_initFields() {
    PowerCubeLevel_initFields = function () {
    };
    PowerCubeLevel$ONE_instance = new PowerCubeLevel('ONE', 0, 1, 1000);
    PowerCubeLevel$TWO_instance = new PowerCubeLevel('TWO', 1, 2, 2000);
    PowerCubeLevel$THREE_instance = new PowerCubeLevel('THREE', 2, 3, 3000);
    PowerCubeLevel$FOUR_instance = new PowerCubeLevel('FOUR', 3, 4, 4000);
    PowerCubeLevel$FIVE_instance = new PowerCubeLevel('FIVE', 4, 5, 5000);
    PowerCubeLevel$SIX_instance = new PowerCubeLevel('SIX', 5, 6, 6000);
    PowerCubeLevel$SEVEN_instance = new PowerCubeLevel('SEVEN', 6, 7, 7000);
    PowerCubeLevel$EIGHT_instance = new PowerCubeLevel('EIGHT', 7, 8, 8000);
    PowerCubeLevel$Companion_getInstance();
  }
  var PowerCubeLevel$ONE_instance;
  function PowerCubeLevel$ONE_getInstance() {
    PowerCubeLevel_initFields();
    return PowerCubeLevel$ONE_instance;
  }
  var PowerCubeLevel$TWO_instance;
  function PowerCubeLevel$TWO_getInstance() {
    PowerCubeLevel_initFields();
    return PowerCubeLevel$TWO_instance;
  }
  var PowerCubeLevel$THREE_instance;
  function PowerCubeLevel$THREE_getInstance() {
    PowerCubeLevel_initFields();
    return PowerCubeLevel$THREE_instance;
  }
  var PowerCubeLevel$FOUR_instance;
  function PowerCubeLevel$FOUR_getInstance() {
    PowerCubeLevel_initFields();
    return PowerCubeLevel$FOUR_instance;
  }
  var PowerCubeLevel$FIVE_instance;
  function PowerCubeLevel$FIVE_getInstance() {
    PowerCubeLevel_initFields();
    return PowerCubeLevel$FIVE_instance;
  }
  var PowerCubeLevel$SIX_instance;
  function PowerCubeLevel$SIX_getInstance() {
    PowerCubeLevel_initFields();
    return PowerCubeLevel$SIX_instance;
  }
  var PowerCubeLevel$SEVEN_instance;
  function PowerCubeLevel$SEVEN_getInstance() {
    PowerCubeLevel_initFields();
    return PowerCubeLevel$SEVEN_instance;
  }
  var PowerCubeLevel$EIGHT_instance;
  function PowerCubeLevel$EIGHT_getInstance() {
    PowerCubeLevel_initFields();
    return PowerCubeLevel$EIGHT_instance;
  }
  PowerCubeLevel.prototype.calculateRecycleXm = function () {
    return this.xmValue_5hxmsq$_0;
  };
  PowerCubeLevel.prototype.toInt = function () {
    return this.level;
  };
  PowerCubeLevel.prototype.getColor = function () {
    var tmp$;
    return (tmp$ = LevelColor_getInstance().map.get_11rb$(this.level)) != null ? tmp$ : '#FFFFFF';
  };
  function PowerCubeLevel$Companion() {
    PowerCubeLevel$Companion_instance = this;
  }
  PowerCubeLevel$Companion.prototype.find_p76lt3$ = function (level, quality) {
    return this.valueOf_za3lpa$(this.clipLevel_0(level + quality.addLevels | 0));
  };
  PowerCubeLevel$Companion.prototype.valueOf_za3lpa$ = function (level) {
    var $receiver = PowerCubeLevel$values();
    var firstOrNull$result;
    firstOrNull$break: do {
      var tmp$;
      for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
        var element = $receiver[tmp$];
        if (element.level === this.clipLevel_0(level)) {
          firstOrNull$result = element;
          break firstOrNull$break;
        }
      }
      firstOrNull$result = null;
    }
     while (false);
    return ensureNotNull(firstOrNull$result);
  };
  PowerCubeLevel$Companion.prototype.clipLevel_0 = function (level) {
    var b = Math_0.min(level, 8);
    return Math_0.max(1, b);
  };
  PowerCubeLevel$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var PowerCubeLevel$Companion_instance = null;
  function PowerCubeLevel$Companion_getInstance() {
    PowerCubeLevel_initFields();
    if (PowerCubeLevel$Companion_instance === null) {
      new PowerCubeLevel$Companion();
    }
    return PowerCubeLevel$Companion_instance;
  }
  PowerCubeLevel.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'PowerCubeLevel',
    interfaces: [ItemLevel, Enum]
  };
  function PowerCubeLevel$values() {
    return [PowerCubeLevel$ONE_getInstance(), PowerCubeLevel$TWO_getInstance(), PowerCubeLevel$THREE_getInstance(), PowerCubeLevel$FOUR_getInstance(), PowerCubeLevel$FIVE_getInstance(), PowerCubeLevel$SIX_getInstance(), PowerCubeLevel$SEVEN_getInstance(), PowerCubeLevel$EIGHT_getInstance()];
  }
  PowerCubeLevel.values = PowerCubeLevel$values;
  function PowerCubeLevel$valueOf(name) {
    switch (name) {
      case 'ONE':
        return PowerCubeLevel$ONE_getInstance();
      case 'TWO':
        return PowerCubeLevel$TWO_getInstance();
      case 'THREE':
        return PowerCubeLevel$THREE_getInstance();
      case 'FOUR':
        return PowerCubeLevel$FOUR_getInstance();
      case 'FIVE':
        return PowerCubeLevel$FIVE_getInstance();
      case 'SIX':
        return PowerCubeLevel$SIX_getInstance();
      case 'SEVEN':
        return PowerCubeLevel$SEVEN_getInstance();
      case 'EIGHT':
        return PowerCubeLevel$EIGHT_getInstance();
      default:throwISE('No enum constant items.level.PowerCubeLevel.' + name);
    }
  }
  PowerCubeLevel.valueOf_61zpoe$ = PowerCubeLevel$valueOf;
  function ResonatorLevel(name, ordinal, level, deployablePerPlayer, energy) {
    Enum.call(this);
    this.level = level;
    this.deployablePerPlayer = deployablePerPlayer;
    this.energy = energy;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function ResonatorLevel_initFields() {
    ResonatorLevel_initFields = function () {
    };
    ResonatorLevel$ONE_instance = new ResonatorLevel('ONE', 0, 1, 8, 1000);
    ResonatorLevel$TWO_instance = new ResonatorLevel('TWO', 1, 2, 4, 1500);
    ResonatorLevel$THREE_instance = new ResonatorLevel('THREE', 2, 3, 4, 2000);
    ResonatorLevel$FOUR_instance = new ResonatorLevel('FOUR', 3, 4, 4, 2500);
    ResonatorLevel$FIVE_instance = new ResonatorLevel('FIVE', 4, 5, 2, 3000);
    ResonatorLevel$SIX_instance = new ResonatorLevel('SIX', 5, 6, 2, 4000);
    ResonatorLevel$SEVEN_instance = new ResonatorLevel('SEVEN', 6, 7, 1, 5000);
    ResonatorLevel$EIGHT_instance = new ResonatorLevel('EIGHT', 7, 8, 1, 6000);
    ResonatorLevel$Companion_getInstance();
  }
  var ResonatorLevel$ONE_instance;
  function ResonatorLevel$ONE_getInstance() {
    ResonatorLevel_initFields();
    return ResonatorLevel$ONE_instance;
  }
  var ResonatorLevel$TWO_instance;
  function ResonatorLevel$TWO_getInstance() {
    ResonatorLevel_initFields();
    return ResonatorLevel$TWO_instance;
  }
  var ResonatorLevel$THREE_instance;
  function ResonatorLevel$THREE_getInstance() {
    ResonatorLevel_initFields();
    return ResonatorLevel$THREE_instance;
  }
  var ResonatorLevel$FOUR_instance;
  function ResonatorLevel$FOUR_getInstance() {
    ResonatorLevel_initFields();
    return ResonatorLevel$FOUR_instance;
  }
  var ResonatorLevel$FIVE_instance;
  function ResonatorLevel$FIVE_getInstance() {
    ResonatorLevel_initFields();
    return ResonatorLevel$FIVE_instance;
  }
  var ResonatorLevel$SIX_instance;
  function ResonatorLevel$SIX_getInstance() {
    ResonatorLevel_initFields();
    return ResonatorLevel$SIX_instance;
  }
  var ResonatorLevel$SEVEN_instance;
  function ResonatorLevel$SEVEN_getInstance() {
    ResonatorLevel_initFields();
    return ResonatorLevel$SEVEN_instance;
  }
  var ResonatorLevel$EIGHT_instance;
  function ResonatorLevel$EIGHT_getInstance() {
    ResonatorLevel_initFields();
    return ResonatorLevel$EIGHT_instance;
  }
  ResonatorLevel.prototype.calculateRecycleXm = function () {
    return this.level * 20 | 0;
  };
  ResonatorLevel.prototype.toInt = function () {
    return this.level;
  };
  ResonatorLevel.prototype.getColor = function () {
    var tmp$;
    return (tmp$ = LevelColor_getInstance().map.get_11rb$(this.level)) != null ? tmp$ : '#FFFFFF';
  };
  function ResonatorLevel$Companion() {
    ResonatorLevel$Companion_instance = this;
  }
  ResonatorLevel$Companion.prototype.valueOf_za3lpa$ = function (level) {
    var $receiver = ResonatorLevel$values();
    var firstOrNull$result;
    firstOrNull$break: do {
      var tmp$;
      for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
        var element = $receiver[tmp$];
        if (element.level === this.clipLevel_0(level)) {
          firstOrNull$result = element;
          break firstOrNull$break;
        }
      }
      firstOrNull$result = null;
    }
     while (false);
    return ensureNotNull(firstOrNull$result);
  };
  ResonatorLevel$Companion.prototype.find_p76lt3$ = function (level, quality) {
    return this.valueOf_za3lpa$(this.clipLevel_0(level + quality.addLevels | 0));
  };
  ResonatorLevel$Companion.prototype.clipLevel_0 = function (level) {
    var b = Math_0.min(level, 8);
    return Math_0.max(1, b);
  };
  ResonatorLevel$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var ResonatorLevel$Companion_instance = null;
  function ResonatorLevel$Companion_getInstance() {
    ResonatorLevel_initFields();
    if (ResonatorLevel$Companion_instance === null) {
      new ResonatorLevel$Companion();
    }
    return ResonatorLevel$Companion_instance;
  }
  ResonatorLevel.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ResonatorLevel',
    interfaces: [ItemLevel, Enum]
  };
  function ResonatorLevel$values() {
    return [ResonatorLevel$ONE_getInstance(), ResonatorLevel$TWO_getInstance(), ResonatorLevel$THREE_getInstance(), ResonatorLevel$FOUR_getInstance(), ResonatorLevel$FIVE_getInstance(), ResonatorLevel$SIX_getInstance(), ResonatorLevel$SEVEN_getInstance(), ResonatorLevel$EIGHT_getInstance()];
  }
  ResonatorLevel.values = ResonatorLevel$values;
  function ResonatorLevel$valueOf(name) {
    switch (name) {
      case 'ONE':
        return ResonatorLevel$ONE_getInstance();
      case 'TWO':
        return ResonatorLevel$TWO_getInstance();
      case 'THREE':
        return ResonatorLevel$THREE_getInstance();
      case 'FOUR':
        return ResonatorLevel$FOUR_getInstance();
      case 'FIVE':
        return ResonatorLevel$FIVE_getInstance();
      case 'SIX':
        return ResonatorLevel$SIX_getInstance();
      case 'SEVEN':
        return ResonatorLevel$SEVEN_getInstance();
      case 'EIGHT':
        return ResonatorLevel$EIGHT_getInstance();
      default:throwISE('No enum constant items.level.ResonatorLevel.' + name);
    }
  }
  ResonatorLevel.valueOf_61zpoe$ = ResonatorLevel$valueOf;
  function UltraStrikeLevel(name, ordinal, level, damage, rangeM, xmCost) {
    Enum.call(this);
    this.level = level;
    this.damage = damage;
    this.rangeM = rangeM;
    this.xmCost = xmCost;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function UltraStrikeLevel_initFields() {
    UltraStrikeLevel_initFields = function () {
    };
    UltraStrikeLevel$ONE_instance = new UltraStrikeLevel('ONE', 0, 1, 150, 10, 50);
    UltraStrikeLevel$TWO_instance = new UltraStrikeLevel('TWO', 1, 2, 300, 13, 100);
    UltraStrikeLevel$THREE_instance = new UltraStrikeLevel('THREE', 2, 3, 500, 16, 150);
    UltraStrikeLevel$FOUR_instance = new UltraStrikeLevel('FOUR', 3, 4, 900, 18, 200);
    UltraStrikeLevel$FIVE_instance = new UltraStrikeLevel('FIVE', 4, 5, 1200, 21, 250);
    UltraStrikeLevel$SIX_instance = new UltraStrikeLevel('SIX', 5, 6, 1500, 24, 360);
    UltraStrikeLevel$SEVEN_instance = new UltraStrikeLevel('SEVEN', 6, 7, 1800, 27, 490);
    UltraStrikeLevel$EIGHT_instance = new UltraStrikeLevel('EIGHT', 7, 8, 2700, 30, 640);
    UltraStrikeLevel$Companion_getInstance();
  }
  var UltraStrikeLevel$ONE_instance;
  function UltraStrikeLevel$ONE_getInstance() {
    UltraStrikeLevel_initFields();
    return UltraStrikeLevel$ONE_instance;
  }
  var UltraStrikeLevel$TWO_instance;
  function UltraStrikeLevel$TWO_getInstance() {
    UltraStrikeLevel_initFields();
    return UltraStrikeLevel$TWO_instance;
  }
  var UltraStrikeLevel$THREE_instance;
  function UltraStrikeLevel$THREE_getInstance() {
    UltraStrikeLevel_initFields();
    return UltraStrikeLevel$THREE_instance;
  }
  var UltraStrikeLevel$FOUR_instance;
  function UltraStrikeLevel$FOUR_getInstance() {
    UltraStrikeLevel_initFields();
    return UltraStrikeLevel$FOUR_instance;
  }
  var UltraStrikeLevel$FIVE_instance;
  function UltraStrikeLevel$FIVE_getInstance() {
    UltraStrikeLevel_initFields();
    return UltraStrikeLevel$FIVE_instance;
  }
  var UltraStrikeLevel$SIX_instance;
  function UltraStrikeLevel$SIX_getInstance() {
    UltraStrikeLevel_initFields();
    return UltraStrikeLevel$SIX_instance;
  }
  var UltraStrikeLevel$SEVEN_instance;
  function UltraStrikeLevel$SEVEN_getInstance() {
    UltraStrikeLevel_initFields();
    return UltraStrikeLevel$SEVEN_instance;
  }
  var UltraStrikeLevel$EIGHT_instance;
  function UltraStrikeLevel$EIGHT_getInstance() {
    UltraStrikeLevel_initFields();
    return UltraStrikeLevel$EIGHT_instance;
  }
  UltraStrikeLevel.prototype.calculateRecycleXm = function () {
    return this.level * 20 | 0;
  };
  UltraStrikeLevel.prototype.critRate = function () {
    return 0.05;
  };
  UltraStrikeLevel.prototype.critDamage = function () {
    return this.damage * 3 | 0;
  };
  UltraStrikeLevel.prototype.toInt = function () {
    return this.level;
  };
  UltraStrikeLevel.prototype.getColor = function () {
    var tmp$;
    return (tmp$ = LevelColor_getInstance().map.get_11rb$(this.level)) != null ? tmp$ : '#FFFFFF';
  };
  function UltraStrikeLevel$Companion() {
    UltraStrikeLevel$Companion_instance = this;
  }
  UltraStrikeLevel$Companion.prototype.find_p76lt3$ = function (level, quality) {
    return this.valueOf_za3lpa$(this.clipLevel_0(level + quality.addLevels | 0));
  };
  UltraStrikeLevel$Companion.prototype.valueOf_za3lpa$ = function (level) {
    var $receiver = UltraStrikeLevel$values();
    var firstOrNull$result;
    firstOrNull$break: do {
      var tmp$;
      for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
        var element = $receiver[tmp$];
        if (element.level === this.clipLevel_0(level)) {
          firstOrNull$result = element;
          break firstOrNull$break;
        }
      }
      firstOrNull$result = null;
    }
     while (false);
    return ensureNotNull(firstOrNull$result);
  };
  UltraStrikeLevel$Companion.prototype.clipLevel_0 = function (level) {
    var b = Math_0.min(level, 8);
    return Math_0.max(1, b);
  };
  UltraStrikeLevel$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var UltraStrikeLevel$Companion_instance = null;
  function UltraStrikeLevel$Companion_getInstance() {
    UltraStrikeLevel_initFields();
    if (UltraStrikeLevel$Companion_instance === null) {
      new UltraStrikeLevel$Companion();
    }
    return UltraStrikeLevel$Companion_instance;
  }
  UltraStrikeLevel.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'UltraStrikeLevel',
    interfaces: [ItemLevel, Enum]
  };
  function UltraStrikeLevel$values() {
    return [UltraStrikeLevel$ONE_getInstance(), UltraStrikeLevel$TWO_getInstance(), UltraStrikeLevel$THREE_getInstance(), UltraStrikeLevel$FOUR_getInstance(), UltraStrikeLevel$FIVE_getInstance(), UltraStrikeLevel$SIX_getInstance(), UltraStrikeLevel$SEVEN_getInstance(), UltraStrikeLevel$EIGHT_getInstance()];
  }
  UltraStrikeLevel.values = UltraStrikeLevel$values;
  function UltraStrikeLevel$valueOf(name) {
    switch (name) {
      case 'ONE':
        return UltraStrikeLevel$ONE_getInstance();
      case 'TWO':
        return UltraStrikeLevel$TWO_getInstance();
      case 'THREE':
        return UltraStrikeLevel$THREE_getInstance();
      case 'FOUR':
        return UltraStrikeLevel$FOUR_getInstance();
      case 'FIVE':
        return UltraStrikeLevel$FIVE_getInstance();
      case 'SIX':
        return UltraStrikeLevel$SIX_getInstance();
      case 'SEVEN':
        return UltraStrikeLevel$SEVEN_getInstance();
      case 'EIGHT':
        return UltraStrikeLevel$EIGHT_getInstance();
      default:throwISE('No enum constant items.level.UltraStrikeLevel.' + name);
    }
  }
  UltraStrikeLevel.valueOf_61zpoe$ = UltraStrikeLevel$valueOf;
  function XmpLevel(name, ordinal, level, damage, rangeM, xmCost) {
    Enum.call(this);
    this.level = level;
    this.damage = damage;
    this.rangeM = rangeM;
    this.xmCost = xmCost;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function XmpLevel_initFields() {
    XmpLevel_initFields = function () {
    };
    XmpLevel$ONE_instance = new XmpLevel('ONE', 0, 1, 150, 42, 50);
    XmpLevel$TWO_instance = new XmpLevel('TWO', 1, 2, 300, 48, 100);
    XmpLevel$THREE_instance = new XmpLevel('THREE', 2, 3, 500, 58, 150);
    XmpLevel$FOUR_instance = new XmpLevel('FOUR', 3, 4, 900, 72, 200);
    XmpLevel$FIVE_instance = new XmpLevel('FIVE', 4, 5, 1200, 90, 250);
    XmpLevel$SIX_instance = new XmpLevel('SIX', 5, 6, 1500, 112, 360);
    XmpLevel$SEVEN_instance = new XmpLevel('SEVEN', 6, 7, 1800, 138, 490);
    XmpLevel$EIGHT_instance = new XmpLevel('EIGHT', 7, 8, 2700, 168, 640);
    XmpLevel$Companion_getInstance();
  }
  var XmpLevel$ONE_instance;
  function XmpLevel$ONE_getInstance() {
    XmpLevel_initFields();
    return XmpLevel$ONE_instance;
  }
  var XmpLevel$TWO_instance;
  function XmpLevel$TWO_getInstance() {
    XmpLevel_initFields();
    return XmpLevel$TWO_instance;
  }
  var XmpLevel$THREE_instance;
  function XmpLevel$THREE_getInstance() {
    XmpLevel_initFields();
    return XmpLevel$THREE_instance;
  }
  var XmpLevel$FOUR_instance;
  function XmpLevel$FOUR_getInstance() {
    XmpLevel_initFields();
    return XmpLevel$FOUR_instance;
  }
  var XmpLevel$FIVE_instance;
  function XmpLevel$FIVE_getInstance() {
    XmpLevel_initFields();
    return XmpLevel$FIVE_instance;
  }
  var XmpLevel$SIX_instance;
  function XmpLevel$SIX_getInstance() {
    XmpLevel_initFields();
    return XmpLevel$SIX_instance;
  }
  var XmpLevel$SEVEN_instance;
  function XmpLevel$SEVEN_getInstance() {
    XmpLevel_initFields();
    return XmpLevel$SEVEN_instance;
  }
  var XmpLevel$EIGHT_instance;
  function XmpLevel$EIGHT_getInstance() {
    XmpLevel_initFields();
    return XmpLevel$EIGHT_instance;
  }
  XmpLevel.prototype.calculateRecycleXm = function () {
    return this.level * 20 | 0;
  };
  XmpLevel.prototype.toInt = function () {
    return this.level;
  };
  XmpLevel.prototype.getColor = function () {
    var tmp$;
    return (tmp$ = LevelColor_getInstance().map.get_11rb$(this.level)) != null ? tmp$ : '#FFFFFF';
  };
  function XmpLevel$Companion() {
    XmpLevel$Companion_instance = this;
  }
  XmpLevel$Companion.prototype.find_p76lt3$ = function (level, quality) {
    return this.valueOf_za3lpa$(this.clipLevel_0(level + quality.addLevels | 0));
  };
  XmpLevel$Companion.prototype.valueOf_za3lpa$ = function (level) {
    var $receiver = XmpLevel$values();
    var firstOrNull$result;
    firstOrNull$break: do {
      var tmp$;
      for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
        var element = $receiver[tmp$];
        if (element.level === this.clipLevel_0(level)) {
          firstOrNull$result = element;
          break firstOrNull$break;
        }
      }
      firstOrNull$result = null;
    }
     while (false);
    return ensureNotNull(firstOrNull$result);
  };
  XmpLevel$Companion.prototype.clipLevel_0 = function (level) {
    var b = Math_0.min(level, 8);
    return Math_0.max(1, b);
  };
  XmpLevel$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var XmpLevel$Companion_instance = null;
  function XmpLevel$Companion_getInstance() {
    XmpLevel_initFields();
    if (XmpLevel$Companion_instance === null) {
      new XmpLevel$Companion();
    }
    return XmpLevel$Companion_instance;
  }
  XmpLevel.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'XmpLevel',
    interfaces: [ItemLevel, Enum]
  };
  function XmpLevel$values() {
    return [XmpLevel$ONE_getInstance(), XmpLevel$TWO_getInstance(), XmpLevel$THREE_getInstance(), XmpLevel$FOUR_getInstance(), XmpLevel$FIVE_getInstance(), XmpLevel$SIX_getInstance(), XmpLevel$SEVEN_getInstance(), XmpLevel$EIGHT_getInstance()];
  }
  XmpLevel.values = XmpLevel$values;
  function XmpLevel$valueOf(name) {
    switch (name) {
      case 'ONE':
        return XmpLevel$ONE_getInstance();
      case 'TWO':
        return XmpLevel$TWO_getInstance();
      case 'THREE':
        return XmpLevel$THREE_getInstance();
      case 'FOUR':
        return XmpLevel$FOUR_getInstance();
      case 'FIVE':
        return XmpLevel$FIVE_getInstance();
      case 'SIX':
        return XmpLevel$SIX_getInstance();
      case 'SEVEN':
        return XmpLevel$SEVEN_getInstance();
      case 'EIGHT':
        return XmpLevel$EIGHT_getInstance();
      default:throwISE('No enum constant items.level.XmpLevel.' + name);
    }
  }
  XmpLevel.valueOf_61zpoe$ = XmpLevel$valueOf;
  function PowerCube(owner, level) {
    PowerCube$Companion_getInstance();
    this.owner = owner;
    this.level = level;
  }
  PowerCube.prototype.toString = function () {
    return 'PC' + toString(this.level.level);
  };
  PowerCube.prototype.getOwnerId = function () {
    return this.owner.key();
  };
  PowerCube.prototype.getLevel = function () {
    return this.level.level;
  };
  function PowerCube$Companion() {
    PowerCube$Companion_instance = this;
  }
  PowerCube$Companion.prototype.create_tsydwy$ = function (owner, level) {
    return new PowerCube(owner, level);
  };
  PowerCube$Companion.prototype.create_2b7tta$ = function (owner, level) {
    return this.create_tsydwy$(owner, PowerCubeLevel$Companion_getInstance().valueOf_za3lpa$(level));
  };
  PowerCube$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var PowerCube$Companion_instance = null;
  function PowerCube$Companion_getInstance() {
    if (PowerCube$Companion_instance === null) {
      new PowerCube$Companion();
    }
    return PowerCube$Companion_instance;
  }
  PowerCube.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'PowerCube',
    interfaces: [DeployableItem]
  };
  PowerCube.prototype.component1 = function () {
    return this.owner;
  };
  PowerCube.prototype.component2 = function () {
    return this.level;
  };
  PowerCube.prototype.copy_tsydwy$ = function (owner, level) {
    return new PowerCube(owner === void 0 ? this.owner : owner, level === void 0 ? this.level : level);
  };
  PowerCube.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.owner) | 0;
    result = result * 31 + Kotlin.hashCode(this.level) | 0;
    return result;
  };
  PowerCube.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.owner, other.owner) && Kotlin.equals(this.level, other.level)))));
  };
  function QgressItem() {
  }
  QgressItem.$metadata$ = {
    kind: Kind_INTERFACE,
    simpleName: 'QgressItem',
    interfaces: []
  };
  function LinkAmpType(name, ordinal, abbr, color) {
    Enum.call(this);
    this.abbr = abbr;
    this.color = color;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function LinkAmpType_initFields() {
    LinkAmpType_initFields = function () {
    };
    LinkAmpType$RARE_instance = new LinkAmpType('RARE', 0, 'LA', '8cffbf');
    LinkAmpType$VERY_RARE_instance = new LinkAmpType('VERY_RARE', 1, 'VRLA', 'b08cff');
  }
  var LinkAmpType$RARE_instance;
  function LinkAmpType$RARE_getInstance() {
    LinkAmpType_initFields();
    return LinkAmpType$RARE_instance;
  }
  var LinkAmpType$VERY_RARE_instance;
  function LinkAmpType$VERY_RARE_getInstance() {
    LinkAmpType_initFields();
    return LinkAmpType$VERY_RARE_instance;
  }
  LinkAmpType.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'LinkAmpType',
    interfaces: [Enum]
  };
  function LinkAmpType$values() {
    return [LinkAmpType$RARE_getInstance(), LinkAmpType$VERY_RARE_getInstance()];
  }
  LinkAmpType.values = LinkAmpType$values;
  function LinkAmpType$valueOf(name) {
    switch (name) {
      case 'RARE':
        return LinkAmpType$RARE_getInstance();
      case 'VERY_RARE':
        return LinkAmpType$VERY_RARE_getInstance();
      default:throwISE('No enum constant items.types.LinkAmpType.' + name);
    }
  }
  LinkAmpType.valueOf_61zpoe$ = LinkAmpType$valueOf;
  function ModType(name, ordinal, label) {
    Enum.call(this);
    this.label = label;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function ModType_initFields() {
    ModType_initFields = function () {
    };
    ModType$RES_SHIELD_instance = new ModType('RES_SHIELD', 0, 'Shield');
    ModType$MULTIHACK_instance = new ModType('MULTIHACK', 1, 'Multi-hack');
    ModType$FORCE_AMP_instance = new ModType('FORCE_AMP', 2, 'Force Amp');
    ModType$HEATSINK_instance = new ModType('HEATSINK', 3, 'Heat Sink');
    ModType$TURRET_instance = new ModType('TURRET', 4, 'Turret');
    ModType$LINK_AMPLIFIER_instance = new ModType('LINK_AMPLIFIER', 5, 'Link Amp');
  }
  var ModType$RES_SHIELD_instance;
  function ModType$RES_SHIELD_getInstance() {
    ModType_initFields();
    return ModType$RES_SHIELD_instance;
  }
  var ModType$MULTIHACK_instance;
  function ModType$MULTIHACK_getInstance() {
    ModType_initFields();
    return ModType$MULTIHACK_instance;
  }
  var ModType$FORCE_AMP_instance;
  function ModType$FORCE_AMP_getInstance() {
    ModType_initFields();
    return ModType$FORCE_AMP_instance;
  }
  var ModType$HEATSINK_instance;
  function ModType$HEATSINK_getInstance() {
    ModType_initFields();
    return ModType$HEATSINK_instance;
  }
  var ModType$TURRET_instance;
  function ModType$TURRET_getInstance() {
    ModType_initFields();
    return ModType$TURRET_instance;
  }
  var ModType$LINK_AMPLIFIER_instance;
  function ModType$LINK_AMPLIFIER_getInstance() {
    ModType_initFields();
    return ModType$LINK_AMPLIFIER_instance;
  }
  ModType.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ModType',
    interfaces: [Enum]
  };
  function ModType$values() {
    return [ModType$RES_SHIELD_getInstance(), ModType$MULTIHACK_getInstance(), ModType$FORCE_AMP_getInstance(), ModType$HEATSINK_getInstance(), ModType$TURRET_getInstance(), ModType$LINK_AMPLIFIER_getInstance()];
  }
  ModType.values = ModType$values;
  function ModType$valueOf(name) {
    switch (name) {
      case 'RES_SHIELD':
        return ModType$RES_SHIELD_getInstance();
      case 'MULTIHACK':
        return ModType$MULTIHACK_getInstance();
      case 'FORCE_AMP':
        return ModType$FORCE_AMP_getInstance();
      case 'HEATSINK':
        return ModType$HEATSINK_getInstance();
      case 'TURRET':
        return ModType$TURRET_getInstance();
      case 'LINK_AMPLIFIER':
        return ModType$LINK_AMPLIFIER_getInstance();
      default:throwISE('No enum constant items.types.ModType.' + name);
    }
  }
  ModType.valueOf_61zpoe$ = ModType$valueOf;
  function MultihackType(name, ordinal, abbr, color, order, additionalHacks, xmCost, recyclingXm) {
    Enum.call(this);
    this.abbr = abbr;
    this.color = color;
    this.order = order;
    this.additionalHacks = additionalHacks;
    this.xmCost = xmCost;
    this.recyclingXm = recyclingXm;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function MultihackType_initFields() {
    MultihackType_initFields = function () {
    };
    MultihackType$COMMON_instance = new MultihackType('COMMON', 0, 'MH', '8cffbf', 2, 4, 400, 40);
    MultihackType$RARE_instance = new MultihackType('RARE', 1, 'RMH', '73a8ff', 1, 8, 800, 80);
    MultihackType$VERY_RARE_instance = new MultihackType('VERY_RARE', 2, 'VRMH', 'b08cff', 0, 12, 1000, 100);
  }
  var MultihackType$COMMON_instance;
  function MultihackType$COMMON_getInstance() {
    MultihackType_initFields();
    return MultihackType$COMMON_instance;
  }
  var MultihackType$RARE_instance;
  function MultihackType$RARE_getInstance() {
    MultihackType_initFields();
    return MultihackType$RARE_instance;
  }
  var MultihackType$VERY_RARE_instance;
  function MultihackType$VERY_RARE_getInstance() {
    MultihackType_initFields();
    return MultihackType$VERY_RARE_instance;
  }
  MultihackType.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'MultihackType',
    interfaces: [Enum]
  };
  function MultihackType$values() {
    return [MultihackType$COMMON_getInstance(), MultihackType$RARE_getInstance(), MultihackType$VERY_RARE_getInstance()];
  }
  MultihackType.values = MultihackType$values;
  function MultihackType$valueOf(name) {
    switch (name) {
      case 'COMMON':
        return MultihackType$COMMON_getInstance();
      case 'RARE':
        return MultihackType$RARE_getInstance();
      case 'VERY_RARE':
        return MultihackType$VERY_RARE_getInstance();
      default:throwISE('No enum constant items.types.MultihackType.' + name);
    }
  }
  MultihackType.valueOf_61zpoe$ = MultihackType$valueOf;
  function ShieldType(name, ordinal, level, abbr, color, mitigation, stickiness, deployCostXm, chance) {
    Enum.call(this);
    this.level = level;
    this.abbr = abbr;
    this.color = color;
    this.mitigation = mitigation;
    this.stickiness = stickiness;
    this.deployCostXm = deployCostXm;
    this.chance = chance;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function ShieldType_initFields() {
    ShieldType_initFields = function () {
    };
    ShieldType$COMMON_instance = new ShieldType('COMMON', 0, 1, 'CS', '#8CFBBD', 30, 0, 250, 10.0 / 50);
    ShieldType$RARE_instance = new ShieldType('RARE', 1, 2, 'RS', '#B18DFD', 40, 15, 500, 10.0 / 500);
    ShieldType$VERY_RARE_instance = new ShieldType('VERY_RARE', 2, 3, 'VRS', '#F88BF5', 60, 45, 1000, 10.0 / 1500);
    ShieldType$AEGIS_instance = new ShieldType('AEGIS', 3, 4, 'AEGIS', '#F88BF5', 70, 80, 1000, 10.0 / 1500);
    ShieldType$Companion_getInstance();
  }
  var ShieldType$COMMON_instance;
  function ShieldType$COMMON_getInstance() {
    ShieldType_initFields();
    return ShieldType$COMMON_instance;
  }
  var ShieldType$RARE_instance;
  function ShieldType$RARE_getInstance() {
    ShieldType_initFields();
    return ShieldType$RARE_instance;
  }
  var ShieldType$VERY_RARE_instance;
  function ShieldType$VERY_RARE_getInstance() {
    ShieldType_initFields();
    return ShieldType$VERY_RARE_instance;
  }
  var ShieldType$AEGIS_instance;
  function ShieldType$AEGIS_getInstance() {
    ShieldType_initFields();
    return ShieldType$AEGIS_instance;
  }
  function ShieldType$Companion() {
    ShieldType$Companion_instance = this;
  }
  ShieldType$Companion.prototype.getColorForLevel_za3lpa$ = function (level) {
    switch (level) {
      case 1:
        return ShieldType$COMMON_getInstance().color;
      case 2:
        return ShieldType$RARE_getInstance().color;
      case 3:
        return ShieldType$VERY_RARE_getInstance().color;
      case 4:
        return ShieldType$AEGIS_getInstance().color;
      default:return '#FFFFFF';
    }
  };
  ShieldType$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var ShieldType$Companion_instance = null;
  function ShieldType$Companion_getInstance() {
    ShieldType_initFields();
    if (ShieldType$Companion_instance === null) {
      new ShieldType$Companion();
    }
    return ShieldType$Companion_instance;
  }
  ShieldType.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ShieldType',
    interfaces: [Enum]
  };
  function ShieldType$values() {
    return [ShieldType$COMMON_getInstance(), ShieldType$RARE_getInstance(), ShieldType$VERY_RARE_getInstance(), ShieldType$AEGIS_getInstance()];
  }
  ShieldType.values = ShieldType$values;
  function ShieldType$valueOf(name) {
    switch (name) {
      case 'COMMON':
        return ShieldType$COMMON_getInstance();
      case 'RARE':
        return ShieldType$RARE_getInstance();
      case 'VERY_RARE':
        return ShieldType$VERY_RARE_getInstance();
      case 'AEGIS':
        return ShieldType$AEGIS_getInstance();
      default:throwISE('No enum constant items.types.ShieldType.' + name);
    }
  }
  ShieldType.valueOf_61zpoe$ = ShieldType$valueOf;
  function VirusType(name, ordinal, abbr, color, roll) {
    Enum.call(this);
    this.abbr = abbr;
    this.color = color;
    this.roll = roll;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function VirusType_initFields() {
    VirusType_initFields = function () {
    };
    VirusType$JARVIS_VIRUS_instance = new VirusType('JARVIS_VIRUS', 0, 'JARVIS', '#03DC03', 2500);
    VirusType$ADA_REFACTOR_instance = new VirusType('ADA_REFACTOR', 1, 'ADA', '#0088FF', 2500);
  }
  var VirusType$JARVIS_VIRUS_instance;
  function VirusType$JARVIS_VIRUS_getInstance() {
    VirusType_initFields();
    return VirusType$JARVIS_VIRUS_instance;
  }
  var VirusType$ADA_REFACTOR_instance;
  function VirusType$ADA_REFACTOR_getInstance() {
    VirusType_initFields();
    return VirusType$ADA_REFACTOR_instance;
  }
  VirusType.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'VirusType',
    interfaces: [Enum]
  };
  function VirusType$values() {
    return [VirusType$JARVIS_VIRUS_getInstance(), VirusType$ADA_REFACTOR_getInstance()];
  }
  VirusType.values = VirusType$values;
  function VirusType$valueOf(name) {
    switch (name) {
      case 'JARVIS_VIRUS':
        return VirusType$JARVIS_VIRUS_getInstance();
      case 'ADA_REFACTOR':
        return VirusType$ADA_REFACTOR_getInstance();
      default:throwISE('No enum constant items.types.VirusType.' + name);
    }
  }
  VirusType.valueOf_61zpoe$ = VirusType$valueOf;
  function UltraStrike(level, owner) {
    this.level = level;
    this.owner = owner;
  }
  UltraStrike.prototype.toString = function () {
    return 'US' + toString(this.level.level);
  };
  UltraStrike.prototype.getOwnerId = function () {
    return this.owner.key();
  };
  UltraStrike.prototype.getLevel = function () {
    return this.level.level;
  };
  UltraStrike.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'UltraStrike',
    interfaces: [DeployableItem]
  };
  UltraStrike.prototype.component1 = function () {
    return this.level;
  };
  UltraStrike.prototype.component2 = function () {
    return this.owner;
  };
  UltraStrike.prototype.copy_wgohym$ = function (level, owner) {
    return new UltraStrike(level === void 0 ? this.level : level, owner === void 0 ? this.owner : owner);
  };
  UltraStrike.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.level) | 0;
    result = result * 31 + Kotlin.hashCode(this.owner) | 0;
    return result;
  };
  UltraStrike.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.level, other.level) && Kotlin.equals(this.owner, other.owner)))));
  };
  function XmpBurster(owner, level) {
    XmpBurster$Companion_getInstance();
    this.owner = owner;
    this.level = level;
  }
  XmpBurster.prototype.calcBaseDamage_0 = function (isCritical) {
    return isCritical ? this.level.damage * 3 | 0 : this.level.damage;
  };
  XmpBurster.prototype.dealDamage_912u9o$ = function (agent) {
    var resosInRange = agent.findResosInAttackRange_3vxbq7$(this.level);
    var destination = ArrayList_init(collectionSizeOrDefault(resosInRange, 10));
    var tmp$;
    tmp$ = resosInRange.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      var tmp$_0 = destination.add_11rb$;
      var tmp$_1;
      var distanceToAgent = ensureNotNull((tmp$_1 = item.coords) != null ? tmp$_1.distanceTo_lfj9be$(agent.pos) : null);
      var fixedDist = distanceToAgent * Dim_getInstance().pixelToMFactor;
      var b = 1.0 - fixedDist / this.level.rangeM;
      var b_0 = Math_0.min(1.0, b);
      var distanceRatio = Math_0.max(0.0, b_0);
      var isCloseEnough = distanceRatio < Constants_getInstance().phi - 1;
      var isCritical = isCloseEnough && Util_getInstance().random() <= XmpBurster$Companion_getInstance().CRIT_RATE;
      var damageValue = numberToInt(this.calcBaseDamage_0(isCritical) * distanceRatio * XmpBurster$Companion_getInstance().GLOBAL_DAMAGE_MULTIPLIER);
      item.takeDamage_2b7tta$(agent, damageValue);
      tmp$_0.call(destination, new Damage(damageValue, ensureNotNull(item.coords), isCritical));
    }
    return destination;
  };
  XmpBurster.prototype.toString = function () {
    return 'XMP' + toString(this.level.level);
  };
  XmpBurster.prototype.getOwnerId = function () {
    return this.owner.key();
  };
  XmpBurster.prototype.getLevel = function () {
    return this.level.level;
  };
  function XmpBurster$Companion() {
    XmpBurster$Companion_instance = this;
    this.GLOBAL_DAMAGE_MULTIPLIER = 0.2;
    this.CRIT_DAMAGE_MULTIPLIER = 3;
    this.CRIT_RATE = 0.2;
  }
  XmpBurster$Companion.prototype.create_xvk381$ = function (owner, level) {
    return new XmpBurster(owner, level);
  };
  XmpBurster$Companion.prototype.create_2b7tta$ = function (owner, level) {
    return this.create_xvk381$(owner, XmpLevel$Companion_getInstance().valueOf_za3lpa$(level));
  };
  XmpBurster$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var XmpBurster$Companion_instance = null;
  function XmpBurster$Companion_getInstance() {
    if (XmpBurster$Companion_instance === null) {
      new XmpBurster$Companion();
    }
    return XmpBurster$Companion_instance;
  }
  XmpBurster.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'XmpBurster',
    interfaces: [DeployableItem]
  };
  XmpBurster.prototype.component1 = function () {
    return this.owner;
  };
  XmpBurster.prototype.component2 = function () {
    return this.level;
  };
  XmpBurster.prototype.copy_xvk381$ = function (owner, level) {
    return new XmpBurster(owner === void 0 ? this.owner : owner, level === void 0 ? this.level : level);
  };
  XmpBurster.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.owner) | 0;
    result = result * 31 + Kotlin.hashCode(this.level) | 0;
    return result;
  };
  XmpBurster.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.owner, other.owner) && Kotlin.equals(this.level, other.level)))));
  };
  function win() {
    return equals(typeof window, 'undefined') ? null : window;
  }
  function main$lambda(it) {
    HtmlUtil_getInstance().load();
    return Unit;
  }
  function main(args) {
    var tmp$;
    (tmp$ = win()) != null ? (tmp$.onload = main$lambda) : null;
  }
  function Cooldown(name, ordinal, seconds) {
    Enum.call(this);
    this.seconds = seconds;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function Cooldown_initFields() {
    Cooldown_initFields = function () {
    };
    Cooldown$BURNOUT_instance = new Cooldown('BURNOUT', 0, 14400);
    Cooldown$FIVE_instance = new Cooldown('FIVE', 1, 300);
    Cooldown$THREE_instance = new Cooldown('THREE', 2, 240);
    Cooldown$TWO_instance = new Cooldown('TWO', 3, 120);
    Cooldown$ONE_instance = new Cooldown('ONE', 4, 60);
    Cooldown$HALF_instance = new Cooldown('HALF', 5, 30);
    Cooldown$MIN_instance = new Cooldown('MIN', 6, 10);
    Cooldown$NONE_instance = new Cooldown('NONE', 7, 0);
    Cooldown$Companion_getInstance();
  }
  var Cooldown$BURNOUT_instance;
  function Cooldown$BURNOUT_getInstance() {
    Cooldown_initFields();
    return Cooldown$BURNOUT_instance;
  }
  var Cooldown$FIVE_instance;
  function Cooldown$FIVE_getInstance() {
    Cooldown_initFields();
    return Cooldown$FIVE_instance;
  }
  var Cooldown$THREE_instance;
  function Cooldown$THREE_getInstance() {
    Cooldown_initFields();
    return Cooldown$THREE_instance;
  }
  var Cooldown$TWO_instance;
  function Cooldown$TWO_getInstance() {
    Cooldown_initFields();
    return Cooldown$TWO_instance;
  }
  var Cooldown$ONE_instance;
  function Cooldown$ONE_getInstance() {
    Cooldown_initFields();
    return Cooldown$ONE_instance;
  }
  var Cooldown$HALF_instance;
  function Cooldown$HALF_getInstance() {
    Cooldown_initFields();
    return Cooldown$HALF_instance;
  }
  var Cooldown$MIN_instance;
  function Cooldown$MIN_getInstance() {
    Cooldown_initFields();
    return Cooldown$MIN_instance;
  }
  var Cooldown$NONE_instance;
  function Cooldown$NONE_getInstance() {
    Cooldown_initFields();
    return Cooldown$NONE_instance;
  }
  Cooldown.prototype.isHackable = function () {
    return this === Cooldown$NONE_getInstance();
  };
  function Cooldown$Companion() {
    Cooldown$Companion_instance = this;
  }
  var get_indices = Kotlin.kotlin.collections.get_indices_m7z4lg$;
  Cooldown$Companion.prototype.valueOf_za3lpa$ = function (seconds) {
    var tmp$;
    var $receiver = Cooldown$values();
    var lastOrNull$result;
    lastOrNull$break: do {
      var tmp$_0;
      tmp$_0 = reversed(get_indices($receiver)).iterator();
      while (tmp$_0.hasNext()) {
        var index = tmp$_0.next();
        var element = $receiver[index];
        if (element.seconds >= seconds) {
          lastOrNull$result = element;
          break lastOrNull$break;
        }
      }
      lastOrNull$result = null;
    }
     while (false);
    return (tmp$ = lastOrNull$result) != null ? tmp$ : Cooldown$NONE_getInstance();
  };
  Cooldown$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Cooldown$Companion_instance = null;
  function Cooldown$Companion_getInstance() {
    Cooldown_initFields();
    if (Cooldown$Companion_instance === null) {
      new Cooldown$Companion();
    }
    return Cooldown$Companion_instance;
  }
  Cooldown.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Cooldown',
    interfaces: [Enum]
  };
  function Cooldown$values() {
    return [Cooldown$BURNOUT_getInstance(), Cooldown$FIVE_getInstance(), Cooldown$THREE_getInstance(), Cooldown$TWO_getInstance(), Cooldown$ONE_getInstance(), Cooldown$HALF_getInstance(), Cooldown$MIN_getInstance(), Cooldown$NONE_getInstance()];
  }
  Cooldown.values = Cooldown$values;
  function Cooldown$valueOf(name) {
    switch (name) {
      case 'BURNOUT':
        return Cooldown$BURNOUT_getInstance();
      case 'FIVE':
        return Cooldown$FIVE_getInstance();
      case 'THREE':
        return Cooldown$THREE_getInstance();
      case 'TWO':
        return Cooldown$TWO_getInstance();
      case 'ONE':
        return Cooldown$ONE_getInstance();
      case 'HALF':
        return Cooldown$HALF_getInstance();
      case 'MIN':
        return Cooldown$MIN_getInstance();
      case 'NONE':
        return Cooldown$NONE_getInstance();
      default:throwISE('No enum constant portal.Cooldown.' + name);
    }
  }
  Cooldown.valueOf_61zpoe$ = Cooldown$valueOf;
  function Field(origin, primaryAnchor, secondaryAnchor, owner) {
    Field$Companion_getInstance();
    this.origin = origin;
    this.primaryAnchor = primaryAnchor;
    this.secondaryAnchor = secondaryAnchor;
    this.owner = owner;
    this.idSet_0 = linkedSetOf([this.origin, this.primaryAnchor, this.secondaryAnchor]);
  }
  function Field$weakestPortal$lambda(it) {
    return it.calcHealth();
  }
  var compareBy$lambda_7 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_10(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_10.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_10.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Field.prototype.weakestPortal = function () {
    return last(sortedWith(toList_0(this.idSet_0), new Comparator$ObjectLiteral_10(compareBy$lambda_7(Field$weakestPortal$lambda))));
  };
  function Field$strongestAnchors$lambda(it) {
    return it.calcHealth();
  }
  var compareBy$lambda_8 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_11(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_11.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_11.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Field.prototype.strongestAnchors = function () {
    return take(sortedWith(toList_0(this.idSet_0), new Comparator$ObjectLiteral_11(compareBy$lambda_8(Field$strongestAnchors$lambda))), 2);
  };
  function Field$findFurthestFrom$lambda(closure$portal) {
    return function (it) {
      return (new Line(closure$portal.location, it.location)).calcLength();
    };
  }
  var compareBy$lambda_9 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_12(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_12.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_12.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Field.prototype.findFurthestFrom_hv9zn6$ = function (portal) {
    return first(sortedWith(toList_0(this.idSet_0), new Comparator$ObjectLiteral_12(compareBy$lambda_9(Field$findFurthestFrom$lambda(portal)))));
  };
  Field.prototype.isConnectedTo_hv9zn6$ = function (portal) {
    return this.idSet_0.contains_11rb$(portal);
  };
  Field.prototype.calculateMu = function () {
    return this.calculateArea();
  };
  Field.prototype.isCoveringPortal_hv9zn6$ = function (portal) {
    var isPortalPart = this.isConnectedTo_hv9zn6$(portal);
    if (isPortalPart) {
      return false;
    }
    var dXtoSecondary = portal.x() - this.secondaryAnchor.x();
    var dYtoSecondary = portal.y() - this.secondaryAnchor.y();
    var dXSecondaryToPrimary = this.secondaryAnchor.x() - this.primaryAnchor.x();
    var dYPrimaryToSecondary = this.primaryAnchor.y() - this.secondaryAnchor.y();
    var d = dYPrimaryToSecondary * (this.origin.x() - this.secondaryAnchor.x()) + dXSecondaryToPrimary * (this.origin.y() - this.secondaryAnchor.y());
    var s = dYPrimaryToSecondary * dXtoSecondary + dXSecondaryToPrimary * dYtoSecondary;
    var t = (this.secondaryAnchor.y() - this.origin.y()) * dXtoSecondary + (this.origin.x() - this.secondaryAnchor.x()) * dYtoSecondary;
    if (d < 0)
      return s < 0 && t < 0 && s + t > d;
    return s > 0 && t > 0 && s + t < d;
  };
  Field.prototype.calculateArea = function () {
    var a = (new Line(this.origin.location, this.primaryAnchor.location)).calcLength();
    var b = (new Line(this.origin.location, this.secondaryAnchor.location)).calcLength();
    var c = (new Line(this.primaryAnchor.location, this.secondaryAnchor.location)).calcLength();
    var s = (a + b + c) / 2;
    var x = s * (s - a) * (s - b) * (s - c);
    var area = numberToInt(Math_0.sqrt(x));
    var b_0 = area / 100 | 0;
    return Math_0.max(1, b_0);
  };
  function Field$draw$drawCenter(closure$ctx, closure$fullStyle) {
    return function (one, two, three) {
      var receiver = closure$ctx;
      receiver.fillStyle = closure$fullStyle;
      receiver.beginPath();
      receiver.moveTo(one.x, one.y);
      receiver.lineTo(two.x, two.y);
      receiver.lineTo(three.x, three.y);
      receiver.fill();
      receiver.closePath();
    };
  }
  function Field$draw$drawLinear$calcStyle(this$Field) {
    return function (health) {
      return this$Field.owner.faction.fieldStyle + toString(Styles_getInstance().fieldTransparency * health / 100) + ')';
    };
  }
  function Field$draw$drawLinear(this$Field, closure$ctx, closure$fullStyle) {
    return function (portal, first, second) {
      var calcStyle = Field$draw$drawLinear$calcStyle(this$Field);
      var originHp = calcStyle(portal.calcHealth());
      var receiver = closure$ctx;
      var closure$fullStyle_0 = closure$fullStyle;
      var point = (new Line(first, second)).findClosestPointTo_lfj9be$(portal.location);
      var gradient = World_getInstance().ctx().createLinearGradient(portal.x(), portal.y(), point.x, point.y);
      gradient.addColorStop(0.1, originHp);
      gradient.addColorStop(1.0, closure$fullStyle_0);
      receiver.fillStyle = gradient;
      receiver.beginPath();
      receiver.moveTo(portal.x(), portal.y());
      receiver.lineTo(first.x, first.y);
      receiver.lineTo(second.x, second.y);
      receiver.fill();
      receiver.closePath();
    };
  }
  Field.prototype.draw_f69bme$ = function (ctx) {
    var fullStyle = this.owner.faction.fieldStyle + toString(Styles_getInstance().fieldTransparency) + ')';
    var drawCenter = Field$draw$drawCenter(ctx, fullStyle);
    var drawLinear = Field$draw$drawLinear(this, ctx, fullStyle);
    var originAndPrimary = (new Line(this.origin.location, this.primaryAnchor.location)).center();
    var primaryAndSecondary = (new Line(this.primaryAnchor.location, this.secondaryAnchor.location)).center();
    var secondaryAndOrigin = (new Line(this.secondaryAnchor.location, this.origin.location)).center();
    drawCenter(originAndPrimary, primaryAndSecondary, secondaryAndOrigin);
    drawLinear(this.origin, originAndPrimary, secondaryAndOrigin);
    drawLinear(this.primaryAnchor, originAndPrimary, primaryAndSecondary);
    drawLinear(this.secondaryAnchor, secondaryAndOrigin, primaryAndSecondary);
  };
  Field.prototype.toString = function () {
    return this.calculateArea().toString() + 'MU';
  };
  Field.prototype.equals = function (other) {
    return Kotlin.isType(other, Field) && this.idSet_0.containsAll_brywnq$(other.idSet_0);
  };
  Field.prototype.hashCode = function () {
    var $receiver = this.idSet_0;
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(item.hashCode() / 3 | 0);
    }
    return sum(destination);
  };
  function Field$Companion() {
    Field$Companion_instance = this;
    this.destroyAp = 750;
  }
  Field$Companion.prototype.isPossible_rsiz9u$ = function (origin, primaryAnchor, secondaryAnchor) {
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var list = element.fields;
      addAll(destination, list);
    }
    var none$result;
    none$break: do {
      var tmp$_0;
      if (Kotlin.isType(destination, Collection) && destination.isEmpty()) {
        none$result = true;
        break none$break;
      }
      tmp$_0 = destination.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        if (equals(element_0.idSet_0, linkedSetOf([origin, primaryAnchor, secondaryAnchor]))) {
          none$result = false;
          break none$break;
        }
      }
      none$result = true;
    }
     while (false);
    return none$result;
  };
  Field$Companion.prototype.create_veg84i$ = function (origin, primaryAnchor, secondaryAnchor, owner) {
    if (!(!(origin != null ? origin.equals(primaryAnchor) : null) && !(origin != null ? origin.equals(secondaryAnchor) : null) && !(primaryAnchor != null ? primaryAnchor.equals(secondaryAnchor) : null))) {
      var message = 'Check failed.';
      throw IllegalStateException_init(message.toString());
    }
    if (!(owner.faction !== Faction$NONE_getInstance())) {
      var message_0 = 'Check failed.';
      throw IllegalStateException_init(message_0.toString());
    }
    if (this.isPossible_rsiz9u$(origin, primaryAnchor, secondaryAnchor)) {
      return new Field(origin, primaryAnchor, secondaryAnchor, owner);
    }
    return null;
  };
  Field$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Field$Companion_instance = null;
  function Field$Companion_getInstance() {
    if (Field$Companion_instance === null) {
      new Field$Companion();
    }
    return Field$Companion_instance;
  }
  Field.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Field',
    interfaces: []
  };
  Field.prototype.component1 = function () {
    return this.origin;
  };
  Field.prototype.component2 = function () {
    return this.primaryAnchor;
  };
  Field.prototype.component3 = function () {
    return this.secondaryAnchor;
  };
  Field.prototype.component4 = function () {
    return this.owner;
  };
  Field.prototype.copy_veg84i$ = function (origin, primaryAnchor, secondaryAnchor, owner) {
    return new Field(origin === void 0 ? this.origin : origin, primaryAnchor === void 0 ? this.primaryAnchor : primaryAnchor, secondaryAnchor === void 0 ? this.secondaryAnchor : secondaryAnchor, owner === void 0 ? this.owner : owner);
  };
  function HackResult(items, cooldown) {
    this.items = items;
    this.cooldown = cooldown;
  }
  HackResult.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'HackResult',
    interfaces: []
  };
  HackResult.prototype.component1 = function () {
    return this.items;
  };
  HackResult.prototype.component2 = function () {
    return this.cooldown;
  };
  HackResult.prototype.copy_idffk$ = function (items, cooldown) {
    return new HackResult(items === void 0 ? this.items : items, cooldown === void 0 ? this.cooldown : cooldown);
  };
  HackResult.prototype.toString = function () {
    return 'HackResult(items=' + Kotlin.toString(this.items) + (', cooldown=' + Kotlin.toString(this.cooldown)) + ')';
  };
  HackResult.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.items) | 0;
    result = result * 31 + Kotlin.hashCode(this.cooldown) | 0;
    return result;
  };
  HackResult.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.items, other.items) && Kotlin.equals(this.cooldown, other.cooldown)))));
  };
  function Link(origin, destination, creator) {
    Link$Companion_getInstance();
    this.origin = origin;
    this.destination = destination;
    this.creator = creator;
  }
  Link.prototype.getLine = function () {
    return new Line(this.origin.location, this.destination.location);
  };
  Link.prototype.isConnectedTo_hv9zn6$ = function (portal) {
    var tmp$, tmp$_0;
    return ((tmp$ = this.destination) != null ? tmp$.equals(portal) : null) || ((tmp$_0 = this.origin) != null ? tmp$_0.equals(portal) : null);
  };
  function Link$draw$lambda(it) {
    return it.calcHealth();
  }
  var compareBy$lambda_10 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_13(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_13.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_13.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Link.prototype.draw_f69bme$ = function (ctx) {
    var byHealth = sortedWith(listOf([this.origin, this.destination]), new Comparator$ObjectLiteral_13(compareBy$lambda_10(Link$draw$lambda)));
    var minTransparency = 0.2;
    var lowHpTransparency = Util_getInstance().clipDouble_yvo9jy$(last(byHealth).calcHealth() * 0.01, minTransparency, 1.0);
    var highHpTransparency = Util_getInstance().clipDouble_yvo9jy$(first(byHealth).calcHealth() * 0.01, minTransparency, 1.0);
    var gradient = ctx.createLinearGradient(this.origin.x(), this.origin.y(), this.destination.x(), this.destination.y());
    if (this.origin.calcHealth() < this.destination.calcHealth()) {
      gradient.addColorStop(0.0, this.creator.faction.fieldStyle + toString(highHpTransparency) + ')');
      gradient.addColorStop(1.0, this.creator.faction.fieldStyle + toString(lowHpTransparency) + ')');
    }
     else {
      gradient.addColorStop(0.0, this.creator.faction.fieldStyle + toString(lowHpTransparency) + ')');
      gradient.addColorStop(1.0, this.creator.faction.fieldStyle + toString(highHpTransparency) + ')');
    }
    ctx.strokeStyle = gradient;
    ctx.lineWidth = Dim_getInstance().linkLineWidth;
    ctx.beginPath();
    ctx.moveTo(this.getLine().from.x, this.getLine().from.y);
    ctx.lineTo(this.getLine().to.x, this.getLine().to.y);
    ctx.closePath();
    ctx.stroke();
  };
  Link.prototype.toString = function () {
    return this.origin.toString() + ' --> ' + this.destination.toString();
  };
  Link.prototype.equals = function (other) {
    var tmp$, tmp$_0, tmp$_1, tmp$_2;
    return Kotlin.isType(other, Link) && (((tmp$ = this.origin) != null ? tmp$.equals(other.origin) : null) && ((tmp$_0 = this.destination) != null ? tmp$_0.equals(other.destination) : null) || (((tmp$_1 = this.origin) != null ? tmp$_1.equals(other.destination) : null) && ((tmp$_2 = this.destination) != null ? tmp$_2.equals(other.origin) : null)));
  };
  Link.prototype.hashCode = function () {
    return this.origin.hashCode() + this.destination.hashCode() | 0;
  };
  function Link$Companion() {
    Link$Companion_instance = this;
    this.destroyAp = 187;
  }
  Link$Companion.prototype.isNotExisting_4tp95w$ = function (link) {
    var $receiver = World_getInstance().allLinks();
    var none$result;
    none$break: do {
      var tmp$;
      if (Kotlin.isType($receiver, Collection) && $receiver.isEmpty()) {
        none$result = true;
        break none$break;
      }
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        if (element != null ? element.equals(link) : null) {
          none$result = false;
          break none$break;
        }
      }
      none$result = true;
    }
     while (false);
    return none$result;
  };
  Link$Companion.prototype.create_6ezwqo$ = function (origin, destination, linker) {
    if (!!(origin != null ? origin.equals(destination) : null)) {
      var message = 'Check failed.';
      throw IllegalStateException_init(message.toString());
    }
    if (!(linker.faction !== Faction$NONE_getInstance())) {
      var message_0 = 'Check failed.';
      throw IllegalStateException_init(message_0.toString());
    }
    var newLink = new Link(origin, destination, linker);
    if (this.isNotExisting_4tp95w$(newLink)) {
      return newLink;
    }
    return null;
  };
  Link$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Link$Companion_instance = null;
  function Link$Companion_getInstance() {
    if (Link$Companion_instance === null) {
      new Link$Companion();
    }
    return Link$Companion_instance;
  }
  Link.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Link',
    interfaces: []
  };
  Link.prototype.component1 = function () {
    return this.origin;
  };
  Link.prototype.component2 = function () {
    return this.destination;
  };
  Link.prototype.component3 = function () {
    return this.creator;
  };
  Link.prototype.copy_6ezwqo$ = function (origin, destination, creator) {
    return new Link(origin === void 0 ? this.origin : origin, destination === void 0 ? this.destination : destination, creator === void 0 ? this.creator : creator);
  };
  function LinkResult(link, maybeFields) {
    this.link = link;
    this.maybeFields = maybeFields;
  }
  LinkResult.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'LinkResult',
    interfaces: []
  };
  LinkResult.prototype.component1 = function () {
    return this.link;
  };
  LinkResult.prototype.component2 = function () {
    return this.maybeFields;
  };
  LinkResult.prototype.copy_vvflfg$ = function (link, maybeFields) {
    return new LinkResult(link === void 0 ? this.link : link, maybeFields === void 0 ? this.maybeFields : maybeFields);
  };
  LinkResult.prototype.toString = function () {
    return 'LinkResult(link=' + Kotlin.toString(this.link) + (', maybeFields=' + Kotlin.toString(this.maybeFields)) + ')';
  };
  LinkResult.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.link) | 0;
    result = result * 31 + Kotlin.hashCode(this.maybeFields) | 0;
    return result;
  };
  LinkResult.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.link, other.link) && Kotlin.equals(this.maybeFields, other.maybeFields)))));
  };
  function ModSlot(name, ordinal) {
    Enum.call(this);
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function ModSlot_initFields() {
    ModSlot_initFields = function () {
    };
    ModSlot$FIRST_instance = new ModSlot('FIRST', 0);
    ModSlot$SECOND_instance = new ModSlot('SECOND', 1);
    ModSlot$THIRD_instance = new ModSlot('THIRD', 2);
    ModSlot$FOURTH_instance = new ModSlot('FOURTH', 3);
  }
  var ModSlot$FIRST_instance;
  function ModSlot$FIRST_getInstance() {
    ModSlot_initFields();
    return ModSlot$FIRST_instance;
  }
  var ModSlot$SECOND_instance;
  function ModSlot$SECOND_getInstance() {
    ModSlot_initFields();
    return ModSlot$SECOND_instance;
  }
  var ModSlot$THIRD_instance;
  function ModSlot$THIRD_getInstance() {
    ModSlot_initFields();
    return ModSlot$THIRD_instance;
  }
  var ModSlot$FOURTH_instance;
  function ModSlot$FOURTH_getInstance() {
    ModSlot_initFields();
    return ModSlot$FOURTH_instance;
  }
  ModSlot.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ModSlot',
    interfaces: [Enum]
  };
  function ModSlot$values() {
    return [ModSlot$FIRST_getInstance(), ModSlot$SECOND_getInstance(), ModSlot$THIRD_getInstance(), ModSlot$FOURTH_getInstance()];
  }
  ModSlot.values = ModSlot$values;
  function ModSlot$valueOf(name) {
    switch (name) {
      case 'FIRST':
        return ModSlot$FIRST_getInstance();
      case 'SECOND':
        return ModSlot$SECOND_getInstance();
      case 'THIRD':
        return ModSlot$THIRD_getInstance();
      case 'FOURTH':
        return ModSlot$FOURTH_getInstance();
      default:throwISE('No enum constant portal.ModSlot.' + name);
    }
  }
  ModSlot.valueOf_61zpoe$ = ModSlot$valueOf;
  function Octant(name, ordinal, arrow, angle) {
    Enum.call(this);
    this.arrow = toBoxedChar(arrow);
    this.angle_90b5ws$_0 = angle;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function Octant_initFields() {
    Octant_initFields = function () {
    };
    Octant$E_instance = new Octant('E', 0, 8594, 0 * math.PI / 180);
    Octant$SE_instance = new Octant('SE', 1, 8600, 45 * math.PI / 180);
    Octant$S_instance = new Octant('S', 2, 8595, 90 * math.PI / 180);
    Octant$SW_instance = new Octant('SW', 3, 8601, 135 * math.PI / 180);
    Octant$W_instance = new Octant('W', 4, 8592, 180 * math.PI / 180);
    Octant$NW_instance = new Octant('NW', 5, 8598, 225 * math.PI / 180);
    Octant$N_instance = new Octant('N', 6, 8593, 270 * math.PI / 180);
    Octant$NE_instance = new Octant('NE', 7, 8599, 315 * math.PI / 180);
  }
  var Octant$E_instance;
  function Octant$E_getInstance() {
    Octant_initFields();
    return Octant$E_instance;
  }
  var Octant$SE_instance;
  function Octant$SE_getInstance() {
    Octant_initFields();
    return Octant$SE_instance;
  }
  var Octant$S_instance;
  function Octant$S_getInstance() {
    Octant_initFields();
    return Octant$S_instance;
  }
  var Octant$SW_instance;
  function Octant$SW_getInstance() {
    Octant_initFields();
    return Octant$SW_instance;
  }
  var Octant$W_instance;
  function Octant$W_getInstance() {
    Octant_initFields();
    return Octant$W_instance;
  }
  var Octant$NW_instance;
  function Octant$NW_getInstance() {
    Octant_initFields();
    return Octant$NW_instance;
  }
  var Octant$N_instance;
  function Octant$N_getInstance() {
    Octant_initFields();
    return Octant$N_instance;
  }
  var Octant$NE_instance;
  function Octant$NE_getInstance() {
    Octant_initFields();
    return Octant$NE_instance;
  }
  Octant.prototype.calcXOffset_za3lpa$ = function (radius) {
    var x = this.angle_90b5ws$_0;
    return numberToInt(radius * Math_0.cos(x));
  };
  Octant.prototype.calcYOffset_za3lpa$ = function (radius) {
    var x = this.angle_90b5ws$_0;
    return numberToInt(radius * Math_0.sin(x));
  };
  Octant.prototype.toString = function () {
    return String.fromCharCode(unboxChar(this.arrow));
  };
  Octant.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Octant',
    interfaces: [Enum]
  };
  function Octant$values() {
    return [Octant$E_getInstance(), Octant$SE_getInstance(), Octant$S_getInstance(), Octant$SW_getInstance(), Octant$W_getInstance(), Octant$NW_getInstance(), Octant$N_getInstance(), Octant$NE_getInstance()];
  }
  Octant.values = Octant$values;
  function Octant$valueOf(name) {
    switch (name) {
      case 'E':
        return Octant$E_getInstance();
      case 'SE':
        return Octant$SE_getInstance();
      case 'S':
        return Octant$S_getInstance();
      case 'SW':
        return Octant$SW_getInstance();
      case 'W':
        return Octant$W_getInstance();
      case 'NW':
        return Octant$NW_getInstance();
      case 'N':
        return Octant$N_getInstance();
      case 'NE':
        return Octant$NE_getInstance();
      default:throwISE('No enum constant portal.Octant.' + name);
    }
  }
  Octant.valueOf_61zpoe$ = Octant$valueOf;
  function Portal(name, location, heatMap, vectorField, resoSlots, links, fields, owner) {
    Portal$Companion_getInstance();
    this.name = name;
    this.location = location;
    this.heatMap = heatMap;
    this.vectorField = vectorField;
    this.resoSlots = resoSlots;
    this.links = links;
    this.fields = fields;
    this.owner = owner;
    this.lastHacks_0 = LinkedHashMap_init();
    this.id = 'P-' + toString(this.location.x) + ':' + toString(this.location.y) + '-' + this.name;
    this.nameImage_0 = HtmlUtil_getInstance().isRunningInBrowser() ? this.createNameImage_0() : null;
  }
  Portal.prototype.isDeprecated = function () {
    return this.resoSlots.isEmpty();
  };
  Portal.prototype.isUncaptured = function () {
    return this.owner == null;
  };
  Portal.prototype.isEnemyOf_912u9o$ = function (agent) {
    var tmp$;
    return this.owner != null && !equals((tmp$ = this.owner) != null ? tmp$.faction : null, agent.faction);
  };
  Portal.prototype.isFriendlyTo_912u9o$ = function (agent) {
    var tmp$;
    return this.owner != null && equals((tmp$ = this.owner) != null ? tmp$.faction : null, agent.faction);
  };
  Portal.prototype.isCoveredByField_0 = function () {
    var $receiver = World_getInstance().allFields();
    var any$result;
    any$break: do {
      var tmp$;
      if (Kotlin.isType($receiver, Collection) && $receiver.isEmpty()) {
        any$result = false;
        break any$break;
      }
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        if (element.isCoveringPortal_hv9zn6$(this)) {
          any$result = true;
          break any$break;
        }
      }
      any$result = false;
    }
     while (false);
    return any$result;
  };
  Portal.prototype.isLinkable_0 = function (linker) {
    var tmp$;
    return equals((tmp$ = this.owner) != null ? tmp$.faction : null, linker.faction) && this.isFullyDeployed_0();
  };
  Portal.prototype.isInside_0 = function () {
    var $receiver = this.findConnectedPortals_0();
    var none$result;
    none$break: do {
      var tmp$;
      if (Kotlin.isType($receiver, Collection) && $receiver.isEmpty()) {
        none$result = true;
        break none$break;
      }
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        var $receiver_0 = element.fields;
        var destination = ArrayList_init_0();
        var tmp$_0;
        tmp$_0 = $receiver_0.iterator();
        while (tmp$_0.hasNext()) {
          var element_0 = tmp$_0.next();
          if (element_0.isConnectedTo_hv9zn6$(this))
            destination.add_11rb$(element_0);
        }
        if (destination.size > 1) {
          none$result = false;
          break none$break;
        }
      }
      none$result = true;
    }
     while (false);
    return none$result;
  };
  Portal.prototype.canHack_912u9o$ = function (hacker) {
    return this.handleCooldown_0(hacker, true) === Cooldown$NONE_getInstance();
  };
  Portal.prototype.canLinkOut_912u9o$ = function (linker) {
    var tmp$ = this.isLinkable_0(linker);
    if (tmp$) {
      var tmp$_0 = this.links.isEmpty();
      if (!tmp$_0) {
        tmp$_0 = this.links.size < 8;
      }
      tmp$ = tmp$_0;
    }
    return tmp$ && !this.isCoveredByField_0() && this.isInside_0();
  };
  Portal.prototype.calculateLevel_0 = function () {
    var tmp$;
    if (this.owner == null)
      tmp$ = 1;
    else {
      var tmp$_0 = Portal$Companion_getInstance();
      var $receiver = this.resoSlots.values;
      var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
      var tmp$_1;
      tmp$_1 = $receiver.iterator();
      while (tmp$_1.hasNext()) {
        var item = tmp$_1.next();
        var tmp$_2, tmp$_3, tmp$_4;
        destination.add_11rb$((tmp$_4 = (tmp$_3 = (tmp$_2 = item.resonator) != null ? tmp$_2.level : null) != null ? tmp$_3.level : null) != null ? tmp$_4 : 0);
      }
      tmp$ = tmp$_0.clipLevel_0(sum(destination) / 8 | 0);
    }
    return tmp$;
  };
  Portal.prototype.getLevel = function () {
    return World_getInstance().isReady ? PortalLevel$Companion_getInstance().findByValue_za3lpa$(this.calculateLevel_0()) : PortalLevel$ZERO_getInstance();
  };
  Portal.prototype.x = function () {
    return this.location.x;
  };
  Portal.prototype.y = function () {
    return this.location.y;
  };
  Portal.prototype.getAllResos_0 = function () {
    var $receiver = this.resoSlots;
    var destination = ArrayList_init($receiver.size);
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(item.value.resonator);
    }
    return filterNotNull(destination);
  };
  Portal.prototype.isFullyDeployed_0 = function () {
    return this.getAllResos_0().size === 8;
  };
  Portal.prototype.averageResoLevel_0 = function () {
    var resos = this.getAllResos_0();
    var destination = ArrayList_init(collectionSizeOrDefault(resos, 10));
    var tmp$;
    tmp$ = resos.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(item.level.level);
    }
    return sum(destination) / resos.size;
  };
  Portal.prototype.calculateLinkMitigation_0 = function () {
    var maxMitigation = 95;
    var incoming = this.findIncomingFrom_0();
    var totalLinkCount = incoming.size + this.links.size | 0;
    var tmp$ = 400.0 / 9.0;
    var x = totalLinkCount / math.E;
    var b = numberToInt(round(tmp$ * Math_0.atan(x)));
    return Math_0.min(maxMitigation, b);
  };
  function Portal$findStrongestReso$lambda(it) {
    return Kotlin.imul(it.energy, it.level.level);
  }
  var compareBy$lambda_11 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_14(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_14.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_14.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Portal.prototype.findStrongestReso_0 = function () {
    var resos = this.getAllResos_0();
    if (resos.isEmpty()) {
      return null;
    }
     else {
      return first(sortedWith(resos, new Comparator$ObjectLiteral_14(compareBy$lambda_11(Portal$findStrongestReso$lambda))));
    }
  };
  Portal.prototype.findStrongestResoPos = function () {
    var tmp$;
    return (tmp$ = this.findStrongestReso_0()) != null ? tmp$.coords : null;
  };
  Portal.prototype.calcHealth = function () {
    var resos = this.getAllResos_0();
    var destination = ArrayList_init(collectionSizeOrDefault(resos, 10));
    var tmp$;
    tmp$ = resos.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(item.calcHealthPercent());
    }
    var health = sum(destination) / resos.size | 0;
    return Util_getInstance().clip_qt1dr2$(health, 0, 100);
  };
  Portal.prototype.calcTotalXm_0 = function () {
    var $receiver = this.getAllResos_0();
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(item.energy);
    }
    return sum(destination);
  };
  function Portal$calculateLinkingRangeInMeters$lambda(this$Portal) {
    return function () {
      var x = this$Portal.averageResoLevel_0();
      return this$Portal.isFullyDeployed_0() ? 160 * x * x * x * x : 0.0;
    };
  }
  Portal.prototype.calculateLinkingRangeInMeters = function () {
    return Portal$calculateLinkingRangeInMeters$lambda(this);
  };
  Portal.prototype.findRandomPointNearPortal_za3lpa$ = function (distance) {
    var tmp$, tmp$_0;
    var angle = Util_getInstance().random() * math.PI;
    var xOffset = numberToInt(distance * Math_0.cos(angle));
    var yOffset = numberToInt(distance * Math_0.sin(angle));
    var point = this.location.copy_lu1900$(this.location.x + xOffset, this.location.y + yOffset);
    if (((tmp$ = World_getInstance().grid.get_11rb$(point.toShadowPos())) != null ? tmp$.isPassable : null) === true) {
      tmp$_0 = point;
    }
     else {
      tmp$_0 = this.findRandomPointNearPortal_za3lpa$(distance);
    }
    return tmp$_0;
  };
  Portal.prototype.findConnectedPortals_0 = function () {
    return plus(this.findOutgoingTo_0(), this.findIncomingFrom_0());
  };
  Portal.prototype.findLinkableForKeys_912u9o$ = function (linker) {
    var keyset = ensureNotNull(linker.inventory.findUniqueKeys());
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var list = element.links;
      addAll(destination, list);
    }
    var destination_0 = ArrayList_init_0();
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      if (Link$Companion_getInstance().isNotExisting_4tp95w$(element_0))
        destination_0.add_11rb$(element_0);
    }
    var allLinks = toSet(destination_0);
    var destination_1 = ArrayList_init(collectionSizeOrDefault(keyset, 10));
    var tmp$_1;
    tmp$_1 = keyset.iterator();
    while (tmp$_1.hasNext()) {
      var item = tmp$_1.next();
      destination_1.add_11rb$(item.portal);
    }
    var destination_2 = ArrayList_init_0();
    var tmp$_2;
    tmp$_2 = destination_1.iterator();
    while (tmp$_2.hasNext()) {
      var element_1 = tmp$_2.next();
      var line = new Line(this.location, element_1.location);
      var destination_3 = ArrayList_init_0();
      var tmp$_3;
      tmp$_3 = allLinks.iterator();
      while (tmp$_3.hasNext()) {
        var element_2 = tmp$_3.next();
        if (element_2.getLine().doesIntersect_589y3w$(line))
          destination_3.add_11rb$(element_2);
      }
      if (destination_3.isEmpty())
        destination_2.add_11rb$(element_1);
    }
    var nonIntersecting = destination_2;
    var destination_4 = ArrayList_init_0();
    var tmp$_4;
    tmp$_4 = nonIntersecting.iterator();
    while (tmp$_4.hasNext()) {
      var element_3 = tmp$_4.next();
      if (element_3.isLinkable_0(linker))
        destination_4.add_11rb$(element_3);
    }
    return destination_4;
  };
  Portal.prototype.createLink_g4r5ni$ = function (linker, target) {
    var newLink = Link$Companion_getInstance().create_6ezwqo$(this, target, linker);
    if (newLink != null) {
      this.links.add_11rb$(newLink);
      linker.inventory.consumeKeyToPortal_hv9zn6$(target);
      Com_getInstance().addMessage_61zpoe$(linker.toString() + ' created a link from ' + this + ' to ' + target);
      SoundUtil_getInstance().playLinkingSound_4tp95w$(newLink);
      linker.addAp_za3lpa$(187);
      linker.removeXm_za3lpa$(250);
      var connectedToTarget = target.findConnectedPortals_0();
      var connectedToHere = this.findConnectedPortals_0();
      var destination = ArrayList_init_0();
      var tmp$;
      tmp$ = connectedToTarget.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        if (connectedToHere.contains_11rb$(element))
          destination.add_11rb$(element);
      }
      var anchors = destination;
      var tmp$_0;
      tmp$_0 = anchors.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        if (Field$Companion_getInstance().isPossible_rsiz9u$(this, target, element_0)) {
          var newField = Field$Companion_getInstance().create_veg84i$(this, target, element_0, linker);
          if (newField != null) {
            Com_getInstance().addMessage_61zpoe$(linker.toString() + ' created a field at ' + this + '. +' + toString(newField));
            SoundUtil_getInstance().playFieldingSound_7ltq94$(newField);
            this.fields.add_11rb$(newField);
            linker.addAp_za3lpa$(1250);
          }
        }
      }
    }
  };
  Portal.prototype.tryHack_912u9o$ = function (hacker) {
    var cooldown = this.handleCooldown_0(hacker, false);
    if (cooldown === Cooldown$NONE_getInstance()) {
      var stuff = this.hack_0(hacker);
      return new HackResult(stuff, null);
    }
    return new HackResult(null, cooldown);
  };
  Portal.prototype.tryGlyph_912u9o$ = function (glypher) {
    var tmp$;
    var normal = this.tryHack_912u9o$(glypher);
    if (normal.cooldown == null) {
      var glyphItems = ArrayList_init_0();
      glyphItems.addAll_brywnq$((tmp$ = normal.items) != null ? tmp$ : emptyList());
      glyphItems.addAll_brywnq$(this.hack_0(glypher));
      if (Util_getInstance().random() < glypher.skills.glyphSkill) {
        glyphItems.addAll_brywnq$(this.hack_0(glypher));
      }
      return new HackResult(toList_0(glyphItems), null);
    }
    return new HackResult(null, normal.cooldown);
  };
  Portal.prototype.hack_0 = function (hacker) {
    var tmp$;
    var a = this.calculateLevel_0();
    var b = hacker.getLevel();
    var level = Math_0.min(a, b);
    var newStuff = ArrayList_init_0();
    newStuff.addAll_brywnq$(this.obtainResos_0(hacker, level));
    newStuff.addAll_brywnq$(this.obtainXmps_0(hacker, level));
    newStuff.addAll_brywnq$(this.obtainShields_0(hacker));
    newStuff.addAll_brywnq$(this.obtainVirus_0(hacker));
    newStuff.addAll_brywnq$(this.obtainPowerCubes_0(level, hacker));
    newStuff.add_11rb$(PortalKey$Companion_getInstance().tryHack_gju65e$(this, hacker));
    var isEnemyPortal = this.owner != null && !equals(hacker.faction, (tmp$ = this.owner) != null ? tmp$.faction : null);
    if (isEnemyPortal) {
      hacker.addAp_za3lpa$(100);
      hacker.removeXm_za3lpa$(300 * this.calculateLevel_0() | 0);
    }
     else {
      hacker.removeXm_za3lpa$(50 * this.calculateLevel_0() | 0);
    }
    return toMutableList(filterNotNull(newStuff));
  };
  Portal.prototype.obtainResos_0 = function (hacker, level) {
    var stuff = ArrayList_init_0();
    var $receiver = Quality$values();
    var destination = ArrayList_init($receiver.length);
    var tmp$;
    for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
      var item = $receiver[tmp$];
      var tmp$_0 = destination.add_11rb$;
      var tmp$_1;
      var selectedLevel = ResonatorLevel$Companion_getInstance().find_p76lt3$(level, item).level;
      while (Util_getInstance().random() < item.chance) {
        stuff.add_11rb$(Kotlin.isType(tmp$_1 = Resonator$Companion_getInstance().create_2b7tta$(hacker, selectedLevel), QgressItem) ? tmp$_1 : throwCCE());
      }
      tmp$_0.call(destination, Unit);
    }
    return stuff;
  };
  Portal.prototype.obtainXmps_0 = function (hacker, level) {
    var stuff = ArrayList_init_0();
    var $receiver = Quality$values();
    var destination = ArrayList_init($receiver.length);
    var tmp$;
    for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
      var item = $receiver[tmp$];
      var tmp$_0 = destination.add_11rb$;
      var tmp$_1;
      var selectedLevel = XmpLevel$Companion_getInstance().find_p76lt3$(level, item).level;
      while (Util_getInstance().random() < item.chance) {
        stuff.add_11rb$(Kotlin.isType(tmp$_1 = XmpBurster$Companion_getInstance().create_2b7tta$(hacker, selectedLevel), QgressItem) ? tmp$_1 : throwCCE());
      }
      tmp$_0.call(destination, Unit);
    }
    return stuff;
  };
  Portal.prototype.obtainShields_0 = function (hacker) {
    var stuff = ArrayList_init_0();
    var $receiver = ShieldType$values();
    var tmp$;
    for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
      var element = $receiver[tmp$];
      if (Util_getInstance().random() < element.chance) {
        stuff.add_11rb$(new Shield(element, hacker));
      }
    }
    return stuff;
  };
  Portal.prototype.obtainPowerCubes_0 = function (level, hacker) {
    var stuff = ArrayList_init_0();
    var $receiver = Quality$values();
    var destination = ArrayList_init($receiver.length);
    var tmp$;
    for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
      var item = $receiver[tmp$];
      var tmp$_0 = destination.add_11rb$;
      var tmp$_1;
      var selectedLevel = PowerCubeLevel$Companion_getInstance().find_p76lt3$(level, item).level;
      while (Util_getInstance().random() < item.chance * 0.3) {
        stuff.add_11rb$(Kotlin.isType(tmp$_1 = PowerCube$Companion_getInstance().create_2b7tta$(hacker, selectedLevel), QgressItem) ? tmp$_1 : throwCCE());
      }
      tmp$_0.call(destination, Unit);
    }
    return stuff;
  };
  Portal.prototype.obtainVirus_0 = function (hacker) {
    var stuff = ArrayList_init_0();
    var $receiver = VirusType$values();
    var tmp$;
    for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
      var element = $receiver[tmp$];
      while (Util_getInstance().random() < (1 / element.roll | 0)) {
        stuff.add_11rb$(new Virus(element, hacker));
      }
    }
    return stuff;
  };
  function Portal$handleCooldown$cool(closure$readOnly, this$Portal, closure$key) {
    return function (agentsLastHacks, tickNr) {
      sort(agentsLastHacks);
      var lastHack = last(agentsLastHacks);
      var ticksSinceLastHack = tickNr - lastHack | 0;
      var timeDiff = Time_getInstance().secondsToTicks_za3lpa$(Cooldown$FIVE_getInstance().seconds) - ticksSinceLastHack | 0;
      var cooldown = Cooldown$Companion_getInstance().valueOf_za3lpa$(Time_getInstance().ticksToSeconds_za3lpa$(timeDiff));
      if (cooldown === Cooldown$NONE_getInstance() && !closure$readOnly) {
        agentsLastHacks.add_11rb$(tickNr);
        var $receiver = this$Portal.lastHacks_0;
        var key = closure$key;
        var value = mutableListOf([tickNr]);
        $receiver.put_xwzc9p$(key, value);
      }
      return cooldown;
    };
  }
  function Portal$handleCooldown$burn(closure$readOnly, this$Portal, closure$key) {
    return function (agentsLastHacks, tickNr) {
      var maxBurnoutTicks = Time_getInstance().secondsToTicks_za3lpa$(Cooldown$BURNOUT_getInstance().seconds);
      var maxTickDifference = tickNr - maxBurnoutTicks | 0;
      var $receiver = toList_0(agentsLastHacks);
      var destination = ArrayList_init_0();
      var tmp$;
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        if (element < maxTickDifference)
          destination.add_11rb$(element);
      }
      var isBurnout = destination.size <= 0;
      if (isBurnout) {
        return Cooldown$BURNOUT_getInstance();
      }
       else {
        if (!closure$readOnly) {
          agentsLastHacks.add_11rb$(tickNr);
          var $receiver_0 = this$Portal.lastHacks_0;
          var key = closure$key;
          var value = mutableListOf([tickNr]);
          $receiver_0.put_xwzc9p$(key, value);
        }
        return Cooldown$NONE_getInstance();
      }
    };
  }
  Portal.prototype.handleCooldown_0 = function (hacker, readOnly) {
    var tmp$;
    var key = hacker.key();
    var cool = Portal$handleCooldown$cool(readOnly, this, key);
    var burn = Portal$handleCooldown$burn(readOnly, this, key);
    var isFirstHack = !this.lastHacks_0.containsKey_11rb$(key);
    if (isFirstHack) {
      if (!readOnly) {
        var $receiver = this.lastHacks_0;
        var value = mutableListOf([World_getInstance().tick]);
        $receiver.put_xwzc9p$(key, value);
      }
      tmp$ = Cooldown$NONE_getInstance();
    }
     else {
      var agentsLastHacks = ensureNotNull(this.lastHacks_0.get_11rb$(key));
      if (agentsLastHacks.size < 4) {
        tmp$ = cool(agentsLastHacks, World_getInstance().tick);
      }
       else {
        tmp$ = burn(agentsLastHacks, World_getInstance().tick);
      }
    }
    return tmp$;
  };
  Portal.prototype.deployMods_45mt8d$ = function (deployer, mods) {
    var isCommon = true;
    var isRare = false;
    var isVeryRare = false;
    if (isCommon) {
      deployer.removeXm_za3lpa$(400);
    }
    if (isRare) {
      deployer.removeXm_za3lpa$(800);
    }
    if (isVeryRare) {
      deployer.removeXm_za3lpa$(1000);
    }
  };
  var checkIndexOverflow = Kotlin.kotlin.collections.checkIndexOverflow_za3lpa$;
  Portal.prototype.deploy_en6fu0$ = function (deployer, resos, distance) {
    var isCapture = this.owner == null;
    if (isCapture) {
      this.owner = deployer;
      Com_getInstance().addMessage_61zpoe$(deployer.toString() + ' captured ' + this + '.');
    }
    var $receiver = this.resoSlots;
    var tmp$;
    var result = LinkedHashMap_init();
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var entry = tmp$.next();
      if (!entry.value.isEmpty()) {
        result.put_xwzc9p$(entry.key, entry.value);
      }
    }
    var destination = LinkedHashMap_init();
    var tmp$_0;
    tmp$_0 = result.entries.iterator();
    while (tmp$_0.hasNext()) {
      var element = tmp$_0.next();
      if (!(element.value.resonator == null)) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    var initialResoCount = destination.size;
    var a = resos.size;
    var b = 8 - initialResoCount | 0;
    var firstResoCount = Math_0.max(a, b);
    var tmp$_1, tmp$_0_0;
    var index = 0;
    tmp$_1 = resos.entries.iterator();
    while (tmp$_1.hasNext()) {
      var item = tmp$_1.next();
      var index_0 = checkIndexOverflow((tmp$_0_0 = index, index = tmp$_0_0 + 1 | 0, tmp$_0_0));
      var octant = item.key;
      var resonator = item.value;
      var tmp$_2, tmp$_3;
      var oldReso = this.resoSlots.get_11rb$(octant);
      if (isCapture && index_0 === 0) {
        deployer.addAp_za3lpa$(500);
      }
       else if (index_0 < firstResoCount) {
        deployer.addAp_za3lpa$(125);
      }
       else if (index_0 === firstResoCount && (firstResoCount + initialResoCount | 0) === 8) {
        deployer.addAp_za3lpa$(250);
      }
       else if ((oldReso != null ? oldReso.isOwnedBy_912u9o$(deployer) : null) !== true) {
        deployer.addAp_za3lpa$(65);
      }
      deployer.removeXm_za3lpa$(resonator.level.level * 20 | 0);
      var oldDistance = oldReso != null ? oldReso.distance : null;
      var newDistance = (tmp$_2 = oldDistance === 0 ? distance : oldDistance) != null ? tmp$_2 : distance;
      (tmp$_3 = this.resoSlots.get_11rb$(octant)) != null ? (tmp$_3.deployReso_otfdig$(deployer, resonator, newDistance), Unit) : null;
      var xx = this.location.x + octant.calcXOffset_za3lpa$(newDistance);
      var yy = this.location.y + octant.calcYOffset_za3lpa$(newDistance);
      resonator.deploy_njiqqf$(this, octant, new Coords(xx, yy));
    }
    var tmp$_4 = deployer.inventory;
    var destination_0 = ArrayList_init(resos.size);
    var tmp$_5;
    tmp$_5 = resos.entries.iterator();
    while (tmp$_5.hasNext()) {
      var item_0 = tmp$_5.next();
      destination_0.add_11rb$(item_0.value);
    }
    tmp$_4.consumeResos_tvxik5$(destination_0);
  };
  Portal.prototype.findOutgoingTo_0 = function () {
    var $receiver = this.links;
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(item.destination);
    }
    return destination;
  };
  Portal.prototype.findIncomingLinks_0 = function () {
    var $receiver = World_getInstance().allLinks();
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var tmp$_0;
      if ((tmp$_0 = element.destination) != null ? tmp$_0.equals(this) : null)
        destination.add_11rb$(element);
    }
    return destination;
  };
  Portal.prototype.findIncomingFrom_0 = function () {
    var $receiver = this.findIncomingLinks_0();
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(item.origin);
    }
    return destination;
  };
  Portal.prototype.destroyAllLinksAndFields_0 = function (destroyer) {
    if (destroyer === void 0)
      destroyer = null;
    var $receiver = World_getInstance().allLinks();
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var tmp$_0;
      if ((tmp$_0 = element.destination) != null ? tmp$_0.equals(this) : null)
        destination.add_11rb$(element);
    }
    var tmp$_1;
    tmp$_1 = destination.iterator();
    while (tmp$_1.hasNext()) {
      var element_0 = tmp$_1.next();
      destroyer != null ? (destroyer.addAp_za3lpa$(187), Unit) : null;
      element_0.origin.links.remove_11rb$(element_0);
    }
    var tmp$_2;
    tmp$_2 = this.links.iterator();
    while (tmp$_2.hasNext()) {
      var element_1 = tmp$_2.next();
      destroyer != null ? (destroyer.addAp_za3lpa$(187), Unit) : null;
    }
    this.links.clear();
    var $receiver_0 = World_getInstance().allFields();
    var destination_0 = ArrayList_init_0();
    var tmp$_3;
    tmp$_3 = $receiver_0.iterator();
    while (tmp$_3.hasNext()) {
      var element_2 = tmp$_3.next();
      var tmp$_4;
      if ((tmp$_4 = element_2.primaryAnchor) != null ? tmp$_4.equals(this) : null)
        destination_0.add_11rb$(element_2);
    }
    var tmp$_5;
    tmp$_5 = destination_0.iterator();
    while (tmp$_5.hasNext()) {
      var element_3 = tmp$_5.next();
      destroyer != null ? (destroyer.addAp_za3lpa$(750), Unit) : null;
      element_3.origin.fields.remove_11rb$(element_3);
    }
    var $receiver_1 = World_getInstance().allFields();
    var destination_1 = ArrayList_init_0();
    var tmp$_6;
    tmp$_6 = $receiver_1.iterator();
    while (tmp$_6.hasNext()) {
      var element_4 = tmp$_6.next();
      var tmp$_7;
      if ((tmp$_7 = element_4.secondaryAnchor) != null ? tmp$_7.equals(this) : null)
        destination_1.add_11rb$(element_4);
    }
    var tmp$_8;
    tmp$_8 = destination_1.iterator();
    while (tmp$_8.hasNext()) {
      var element_5 = tmp$_8.next();
      destroyer != null ? (destroyer.addAp_za3lpa$(750), Unit) : null;
      element_5.origin.fields.remove_11rb$(element_5);
    }
    var tmp$_9;
    tmp$_9 = this.fields.iterator();
    while (tmp$_9.hasNext()) {
      var element_6 = tmp$_9.next();
      destroyer != null ? (destroyer.addAp_za3lpa$(750), Unit) : null;
    }
    this.fields.clear();
  };
  Portal.prototype.destroy_4705j1$ = function (destroyer) {
    if (destroyer === void 0)
      destroyer = null;
    this.owner = null;
    var tmp$;
    tmp$ = this.resoSlots.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      element.value.clear();
    }
    this.destroyAllLinksAndFields_0(destroyer);
    var tmp$_0;
    tmp$_0 = World_getInstance().allAgents.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      var tmp$_1;
      if ((tmp$_1 = element_0.actionPortal) != null ? tmp$_1.equals(this) : null) {
        element_0.actionPortal = World_getInstance().randomPortal();
        element_0.action.start_fyi6w8$(ActionItem$Companion_getInstance().WAIT);
      }
    }
  };
  Portal.prototype.remove = function () {
    this.destroy_4705j1$();
    SoundUtil_getInstance().playPortalRemovalSound_lfj9be$(this.location);
    var tmp$;
    tmp$ = World_getInstance().allAgents.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var $receiver = element.inventory.findKeys();
      var destination = ArrayList_init_0();
      var tmp$_0;
      tmp$_0 = $receiver.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var tmp$_1;
        if ((tmp$_1 = element_0.portal) != null ? tmp$_1.equals(this) : null)
          destination.add_11rb$(element_0);
      }
      var portalKeys = toList_0(destination);
      if (portalKeys != null) {
        element.inventory.items.removeAll_brywnq$(portalKeys);
      }
    }
    World_getInstance().allPortals.remove_11rb$(this);
  };
  Portal.prototype.removeReso_j436sm$ = function (octant, destroyer) {
    var tmp$;
    (tmp$ = this.resoSlots.get_11rb$(octant)) != null ? (tmp$.clear(), Unit) : null;
    var $receiver = this.resoSlots;
    var destination = LinkedHashMap_init();
    var tmp$_0;
    tmp$_0 = $receiver.entries.iterator();
    while (tmp$_0.hasNext()) {
      var element = tmp$_0.next();
      if (element.value.resonator != null) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    var numberOfResosLeft = destination.size;
    if (numberOfResosLeft <= 0) {
      this.destroy_4705j1$(destroyer);
    }
     else if (numberOfResosLeft <= 2) {
      this.destroyAllLinksAndFields_0(destroyer);
    }
  };
  Portal.prototype.findAllowedResoLevels_912u9o$ = function (deployer) {
    var tmp$, tmp$_0;
    if (this.owner == null || equals((tmp$ = this.owner) != null ? tmp$.faction : null, deployer.faction)) {
      var $receiver = ResonatorLevel$values();
      var destination = ArrayList_init($receiver.length);
      var tmp$_1;
      for (tmp$_1 = 0; tmp$_1 !== $receiver.length; ++tmp$_1) {
        var item = $receiver[tmp$_1];
        var tmp$_2 = destination.add_11rb$;
        var tmp$_3 = item.deployablePerPlayer;
        var $receiver_0 = this.resoSlots;
        var destination_0 = LinkedHashMap_init();
        var tmp$_4;
        tmp$_4 = $receiver_0.entries.iterator();
        while (tmp$_4.hasNext()) {
          var element = tmp$_4.next();
          var tmp$_5, tmp$_6;
          if (element.value.isOwnedBy_912u9o$(deployer) && ((tmp$_6 = (tmp$_5 = element.value.resonator) != null ? tmp$_5.level : null) != null ? tmp$_6.level : null) === item.level) {
            destination_0.put_xwzc9p$(element.key, element.value);
          }
        }
        tmp$_2.call(destination, to(item, tmp$_3 - destination_0.size | 0));
      }
      tmp$_0 = toMap(destination);
    }
     else {
      tmp$_0 = emptyMap();
    }
    return tmp$_0;
  };
  Portal.prototype.leakXm = function () {
    var tmp$, tmp$_0;
    var fluct = Util_getInstance().randomInt_za3lpa$(300);
    var offset = Util_getInstance().randomBool() ? fluct : -fluct | 0;
    tmp$_0 = this.location;
    if (this.getLevel().toInt() <= 4.5) {
      tmp$ = (this.calculateLevel_0() * 1000 | 0) + offset | 0;
    }
     else {
      tmp$ = (this.calculateLevel_0() * 750 | 0) + offset | 0;
    }
    return to(tmp$_0, tmp$);
  };
  Portal.prototype.decay = function () {
    var allResos = this.getAllResos_0();
    var tmp$;
    tmp$ = allResos.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      element.decay();
    }
    var newResos = this.getAllResos_0();
    if (newResos.isEmpty()) {
      this.destroy_4705j1$();
    }
  };
  function Portal$drawResonators$drawResoLine(closure$ctx) {
    return function (line, levelColor, factionColor, lineWidth, alpha) {
      if (alpha === void 0)
        alpha = 1.0;
      closure$ctx.globalAlpha = alpha;
      closure$ctx.strokeStyle = Colors_getInstance().black;
      closure$ctx.lineWidth = lineWidth + 1.5;
      closure$ctx.beginPath();
      closure$ctx.moveTo(line.from.x, line.from.y);
      closure$ctx.lineTo(line.to.x, line.to.y);
      closure$ctx.closePath();
      closure$ctx.stroke();
      closure$ctx.lineWidth = lineWidth;
      if (Styles_getInstance().isDrawResoLineGradient) {
        var gradient = closure$ctx.createLinearGradient(line.from.x, line.from.y, line.to.x, line.to.y);
        gradient.addColorStop(0.2, levelColor);
        gradient.addColorStop(0.7, factionColor);
        closure$ctx.strokeStyle = gradient;
      }
       else {
        closure$ctx.strokeStyle = levelColor;
      }
      closure$ctx.beginPath();
      closure$ctx.moveTo(line.from.x, line.from.y);
      closure$ctx.lineTo(line.to.x, line.to.y);
      closure$ctx.closePath();
      closure$ctx.stroke();
      closure$ctx.globalAlpha = 1.0;
    };
  }
  Portal.prototype.drawResonators_f69bme$ = function (ctx) {
    if (HtmlUtil_getInstance().isNotRunningInBrowser())
      return;
    var drawResoLine = Portal$drawResonators$drawResoLine(ctx);
    var $receiver = this.resoSlots;
    var destination = LinkedHashMap_init();
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.value.owner != null && element.value.resonator != null) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    var octantSlots = toList(destination);
    var destination_0 = ArrayList_init(collectionSizeOrDefault(octantSlots, 10));
    var tmp$_0;
    tmp$_0 = octantSlots.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      var tmp$_1 = destination_0.add_11rb$;
      var tmp$_2, tmp$_3, tmp$_4;
      var octant = item.first;
      var slot = item.second;
      var reso = ensureNotNull(slot.resonator);
      var resoLevel = reso.level;
      var x = this.location.x + octant.calcXOffset_za3lpa$(slot.distance);
      var y = this.location.y + octant.calcYOffset_za3lpa$(slot.distance);
      var lineToPortal = new Line(new Coords(x, y), this.location);
      var alpha = reso.calcHealthPercent();
      drawResoLine(lineToPortal, resoLevel.getColor(), (tmp$_4 = (tmp$_3 = (tmp$_2 = this.owner) != null ? tmp$_2.faction : null) != null ? tmp$_3.color : null) != null ? tmp$_4 : Faction$NONE_getInstance().color, 1.0, alpha);
      var resoCircle = new Circle(new Coords(x, y), Dim_getInstance().resoRadius);
      DrawUtil_getInstance().drawCircle_3kie0f$(ctx, resoCircle, Colors_getInstance().black, 2.0, resoLevel.getColor(), alpha);
      if (Styles_getInstance().isDrawResoLevels) {
        DrawUtil_getInstance().drawText_omkwws$(ctx, new Coords(x, y), reso.level.level.toString(), Colors_getInstance().black, 8, DrawUtil_getInstance().CODA);
      }
      tmp$_1.call(destination_0, Unit);
    }
  };
  Portal.prototype.drawCenter_j4cg6b$ = function (ctx, isDrawHealthBar) {
    if (isDrawHealthBar === void 0)
      isDrawHealthBar = true;
    var tmp$, tmp$_0, tmp$_1, tmp$_2, tmp$_3, tmp$_4, tmp$_5, tmp$_6;
    if (HtmlUtil_getInstance().isNotRunningInBrowser())
      return;
    tmp$_1 = (tmp$_0 = (tmp$ = this.owner) != null ? tmp$.faction : null) != null ? tmp$_0 : Faction$NONE_getInstance();
    tmp$_2 = this.getLevel();
    var image = Portal$Companion_getInstance().getCenterImage_0(tmp$_1, tmp$_2);
    var x = this.location.x - (image.width / 2 | 0);
    var y = this.location.y - (image.height / 2 | 0);
    ctx.drawImage(image, x, y);
    if (isDrawHealthBar) {
      tmp$_5 = (tmp$_4 = (tmp$_3 = this.owner) != null ? tmp$_3.faction : null) != null ? tmp$_4 : Faction$NONE_getInstance();
      tmp$_6 = this.calcHealth();
      var healthBarImage = Portal$Companion_getInstance().getHealthBarImage_0(tmp$_5, tmp$_6);
      ctx.drawImage(healthBarImage, x, y + image.height + 1);
    }
  };
  Portal.prototype.drawName_f69bme$ = function (ctx) {
    if (HtmlUtil_getInstance().isNotRunningInBrowser())
      return;
    var xOffset = 34;
    var yOffset = 18;
    ctx.drawImage(this.nameImage_0, this.location.x - xOffset, this.location.y + yOffset);
  };
  Portal.prototype.toString = function () {
    return this.name;
  };
  Portal.prototype.equals = function (other) {
    return Kotlin.isType(other, Portal) && equals(this.id, other.id);
  };
  Portal.prototype.hashCode = function () {
    return hashCode(this.id) * 31 | 0;
  };
  function Portal$createNameImage$lambda(closure$x, closure$y, this$Portal, closure$lineWidth) {
    return function (ctx) {
      var coords = Coords_init(numberToInt(closure$x), numberToInt(closure$y));
      DrawUtil_getInstance().strokeText_lowmm9$(ctx, coords, this$Portal.name, Colors_getInstance().white, Dim_getInstance().portalNameFontSize, DrawUtil_getInstance().CODA, closure$lineWidth, Colors_getInstance().black);
    };
  }
  Portal.prototype.createNameImage_0 = function () {
    var fontSize = Dim_getInstance().portalNameFontSize;
    var lineWidth = 2.0;
    var w = 100;
    var h = fontSize + 2 * lineWidth;
    var x = lineWidth + (fontSize / 2 | 0);
    var y = lineWidth + ((fontSize * 2 | 0) / 3 | 0);
    return HtmlUtil_getInstance().preRender_yb5akz$(w, numberToInt(h), Portal$createNameImage$lambda(x, y, this, lineWidth));
  };
  function Portal$Companion() {
    Portal$Companion_instance = this;
    var tmp$, tmp$_0;
    if (HtmlUtil_getInstance().isRunningInBrowser()) {
      var $receiver = PortalLevel$values();
      var destination = ArrayList_init_0();
      var tmp$_1;
      for (tmp$_1 = 0; tmp$_1 !== $receiver.length; ++tmp$_1) {
        var element = $receiver[tmp$_1];
        var $receiver_0 = Faction$values();
        var destination_0 = ArrayList_init($receiver_0.length);
        var tmp$_2;
        for (tmp$_2 = 0; tmp$_2 !== $receiver_0.length; ++tmp$_2) {
          var item = $receiver_0[tmp$_2];
          destination_0.add_11rb$(to(to(item, element), this.renderPortalCenter_wc00gi$(item.color, element)));
        }
        var list = destination_0;
        addAll(destination, list);
      }
      tmp$ = toMap(destination);
    }
     else {
      tmp$ = emptyMap();
    }
    this.centerImages_0 = tmp$;
    if (HtmlUtil_getInstance().isRunningInBrowser()) {
      var $receiver_1 = new IntRange(0, 100);
      var destination_1 = ArrayList_init_0();
      var tmp$_3;
      tmp$_3 = $receiver_1.iterator();
      while (tmp$_3.hasNext()) {
        var element_0 = tmp$_3.next();
        var lw = Dim_getInstance().portalLineWidth;
        var r = numberToInt(Dim_getInstance().portalRadius);
        var w = (r * 2 | 0) + (2 * lw | 0) | 0;
        var $receiver_2 = Faction$values();
        var destination_2 = ArrayList_init($receiver_2.length);
        var tmp$_4;
        for (tmp$_4 = 0; tmp$_4 !== $receiver_2.length; ++tmp$_4) {
          var item_0 = $receiver_2[tmp$_4];
          destination_2.add_11rb$(to(to(item_0, element_0), DrawUtil_getInstance().renderBarImage_ewpgoy$(item_0.color, element_0, 5, w, lw)));
        }
        var list_0 = destination_2;
        addAll(destination_1, list_0);
      }
      tmp$_0 = toMap(destination_1);
    }
     else {
      tmp$_0 = emptyMap();
    }
    this.healthBarImages_0 = tmp$_0;
    this.MAX_HACKS = 4;
  }
  Portal$Companion.prototype.findChargeableForKeys_p3u7jq$ = function (agent, keys) {
    var $receiver = World_getInstance().factionPortals_bip15f$(agent.faction);
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.calcHealth() <= 90)
        destination.add_11rb$(element);
    }
    var chargeable = toSet(destination);
    var destination_0 = ArrayList_init_0();
    var tmp$_0;
    tmp$_0 = chargeable.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      var destination_1 = ArrayList_init(collectionSizeOrDefault(keys, 10));
      var tmp$_1;
      tmp$_1 = keys.iterator();
      while (tmp$_1.hasNext()) {
        var item = tmp$_1.next();
        destination_1.add_11rb$(item.portal);
      }
      if (destination_1.contains_11rb$(element_0))
        destination_0.add_11rb$(element_0);
    }
    return destination_0;
  };
  Portal$Companion.prototype.getCenterImage_0 = function (faction, level) {
    return ensureNotNull(this.centerImages_0.get_11rb$(to(faction, level)));
  };
  Portal$Companion.prototype.getHealthBarImage_0 = function (faction, health) {
    return ensureNotNull(this.healthBarImages_0.get_11rb$(to(faction, health)));
  };
  function Portal$Companion$renderPortalCenter$lambda(closure$r, closure$lw, closure$color, closure$level) {
    return function (ctx) {
      var portalCircle = new Circle(Coords_init(closure$r + closure$lw | 0, closure$r + closure$lw | 0), closure$r);
      DrawUtil_getInstance().drawCircle_3kie0f$(ctx, portalCircle, Colors_getInstance().black, 2.0, closure$color);
      var pos = Coords_init(closure$r + closure$lw + (closure$level.value > 1 ? 0 : 1) | 0, closure$r + closure$lw | 0);
      DrawUtil_getInstance().drawText_omkwws$(ctx, pos, closure$level.display, Colors_getInstance().black, 13, DrawUtil_getInstance().CODA);
    };
  }
  Portal$Companion.prototype.renderPortalCenter_wc00gi$ = function (color, level) {
    var lw = Dim_getInstance().portalLineWidth;
    var r = numberToInt(Dim_getInstance().portalRadius);
    var w = (r * 2 | 0) + (2 * lw | 0) | 0;
    var h = w;
    return HtmlUtil_getInstance().preRender_yb5akz$(w, h, Portal$Companion$renderPortalCenter$lambda(r, lw, color, level));
  };
  Portal$Companion.prototype.clipLevel_0 = function (level) {
    var b = Math_0.min(level, 8);
    return Math_0.max(1, b);
  };
  var LinkedHashSet_init = Kotlin.kotlin.collections.LinkedHashSet_init_287e2$;
  Portal$Companion.prototype.create_lfj9be$ = function (location) {
    var tmp$;
    var $receiver = Octant$values();
    var destination = ArrayList_init($receiver.length);
    var tmp$_0;
    for (tmp$_0 = 0; tmp$_0 !== $receiver.length; ++tmp$_0) {
      var item = $receiver[tmp$_0];
      destination.add_11rb$(to(item, ResonatorSlot$Companion_getInstance().create()));
    }
    var slots = toMutableMap(toMap(destination));
    if (HtmlUtil_getInstance().isRunningInBrowser()) {
      var heatMap = PathUtil_getInstance().generateHeatMap_lfj9be$(location);
      SoundUtil_getInstance().playPortalCreationSound_xv7m3c$(location);
      tmp$ = to(heatMap, PathUtil_getInstance().calculateVectorField_3e8r0f$(heatMap, location));
    }
     else {
      tmp$ = to(LinkedHashMap_init(), LinkedHashMap_init());
    }
    var tmp$_1 = tmp$;
    var heatMap_0 = tmp$_1.component1()
    , vectorField = tmp$_1.component2();
    return new Portal(Util_getInstance().generatePortalName(), location, heatMap_0, vectorField, slots, LinkedHashSet_init(), LinkedHashSet_init(), null);
  };
  Portal$Companion.prototype.createRandom = function () {
    var location = Coords$Companion_getInstance().createRandomForPortal();
    return this.create_lfj9be$(location);
  };
  Portal$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Portal$Companion_instance = null;
  function Portal$Companion_getInstance() {
    if (Portal$Companion_instance === null) {
      new Portal$Companion();
    }
    return Portal$Companion_instance;
  }
  Portal.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Portal',
    interfaces: []
  };
  Portal.prototype.component1 = function () {
    return this.name;
  };
  Portal.prototype.component2 = function () {
    return this.location;
  };
  Portal.prototype.component3 = function () {
    return this.heatMap;
  };
  Portal.prototype.component4 = function () {
    return this.vectorField;
  };
  Portal.prototype.component5 = function () {
    return this.resoSlots;
  };
  Portal.prototype.component6 = function () {
    return this.links;
  };
  Portal.prototype.component7 = function () {
    return this.fields;
  };
  Portal.prototype.component8 = function () {
    return this.owner;
  };
  Portal.prototype.copy_hdvy8s$ = function (name, location, heatMap, vectorField, resoSlots, links, fields, owner) {
    return new Portal(name === void 0 ? this.name : name, location === void 0 ? this.location : location, heatMap === void 0 ? this.heatMap : heatMap, vectorField === void 0 ? this.vectorField : vectorField, resoSlots === void 0 ? this.resoSlots : resoSlots, links === void 0 ? this.links : links, fields === void 0 ? this.fields : fields, owner === void 0 ? this.owner : owner);
  };
  function PortalKey(portal, owner) {
    PortalKey$Companion_getInstance();
    this.portal = portal;
    this.owner = owner;
  }
  PortalKey.prototype.toString = function () {
    return 'Key-' + this.portal;
  };
  PortalKey.prototype.getOwnerId = function () {
    return this.owner.key();
  };
  PortalKey.prototype.getLevel = function () {
    throw new NotImplementedError('Portal Key has no level.');
  };
  PortalKey.prototype.isFriendlyToOwner = function () {
    var tmp$;
    return equals((tmp$ = this.portal.owner) != null ? tmp$.faction : null, this.owner.faction);
  };
  function PortalKey$Companion() {
    PortalKey$Companion_instance = this;
  }
  PortalKey$Companion.prototype.tryHack_gju65e$ = function (portal, agent) {
    if (Util_getInstance().random() < Probabilities_getInstance().keyChance) {
      return new PortalKey(portal, agent);
    }
     else {
      return null;
    }
  };
  PortalKey$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var PortalKey$Companion_instance = null;
  function PortalKey$Companion_getInstance() {
    if (PortalKey$Companion_instance === null) {
      new PortalKey$Companion();
    }
    return PortalKey$Companion_instance;
  }
  PortalKey.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'PortalKey',
    interfaces: [DeployableItem]
  };
  function Quality(name, ordinal, chance, addLevels) {
    Enum.call(this);
    this.chance = chance;
    this.addLevels = addLevels;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function Quality_initFields() {
    Quality_initFields = function () {
    };
    Quality$BEST_instance = new Quality('BEST', 0, 0.1, 2);
    Quality$TOP_instance = new Quality('TOP', 1, 0.3, 1);
    Quality$GOOD_instance = new Quality('GOOD', 2, 0.5, 0);
    Quality$MORE_instance = new Quality('MORE', 3, 0.7, -1);
  }
  var Quality$BEST_instance;
  function Quality$BEST_getInstance() {
    Quality_initFields();
    return Quality$BEST_instance;
  }
  var Quality$TOP_instance;
  function Quality$TOP_getInstance() {
    Quality_initFields();
    return Quality$TOP_instance;
  }
  var Quality$GOOD_instance;
  function Quality$GOOD_getInstance() {
    Quality_initFields();
    return Quality$GOOD_instance;
  }
  var Quality$MORE_instance;
  function Quality$MORE_getInstance() {
    Quality_initFields();
    return Quality$MORE_instance;
  }
  Quality.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Quality',
    interfaces: [Enum]
  };
  function Quality$values() {
    return [Quality$BEST_getInstance(), Quality$TOP_getInstance(), Quality$GOOD_getInstance(), Quality$MORE_getInstance()];
  }
  Quality.values = Quality$values;
  function Quality$valueOf(name) {
    switch (name) {
      case 'BEST':
        return Quality$BEST_getInstance();
      case 'TOP':
        return Quality$TOP_getInstance();
      case 'GOOD':
        return Quality$GOOD_getInstance();
      case 'MORE':
        return Quality$MORE_getInstance();
      default:throwISE('No enum constant portal.Quality.' + name);
    }
  }
  Quality.valueOf_61zpoe$ = Quality$valueOf;
  function ResonatorSlot(owner, resonator, distance) {
    ResonatorSlot$Companion_getInstance();
    this.owner = owner;
    this.resonator = resonator;
    this.distance = distance;
  }
  ResonatorSlot.prototype.isEmpty = function () {
    return this.resonator == null;
  };
  ResonatorSlot.prototype.isOwnedBy_912u9o$ = function (agent) {
    return equals(this.owner, agent);
  };
  ResonatorSlot.prototype.deployReso_otfdig$ = function (deployer, reso, dist) {
    if (!(dist >= Dim_getInstance().minDeploymentRange)) {
      var message = 'Check failed.';
      throw IllegalStateException_init(message.toString());
    }
    if (!(dist <= Dim_getInstance().maxDeploymentRange)) {
      var message_0 = 'Check failed.';
      throw IllegalStateException_init(message_0.toString());
    }
    if (!(deployer.faction !== Faction$NONE_getInstance())) {
      var message_1 = 'Check failed.';
      throw IllegalStateException_init(message_1.toString());
    }
    this.owner = deployer;
    this.resonator = reso;
    this.distance = dist;
  };
  ResonatorSlot.prototype.clear = function () {
    this.owner = null;
    this.resonator = null;
    this.distance = 0;
  };
  ResonatorSlot.prototype.toString = function () {
    return this.resonator != null ? '[' + toString(this.resonator) + ']' : '[]';
  };
  function ResonatorSlot$Companion() {
    ResonatorSlot$Companion_instance = this;
  }
  ResonatorSlot$Companion.prototype.create = function () {
    return new ResonatorSlot(null, null, 0);
  };
  ResonatorSlot$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var ResonatorSlot$Companion_instance = null;
  function ResonatorSlot$Companion_getInstance() {
    if (ResonatorSlot$Companion_instance === null) {
      new ResonatorSlot$Companion();
    }
    return ResonatorSlot$Companion_instance;
  }
  ResonatorSlot.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'ResonatorSlot',
    interfaces: []
  };
  ResonatorSlot.prototype.component1 = function () {
    return this.owner;
  };
  ResonatorSlot.prototype.component2 = function () {
    return this.resonator;
  };
  ResonatorSlot.prototype.component3 = function () {
    return this.distance;
  };
  ResonatorSlot.prototype.copy_pwrer2$ = function (owner, resonator, distance) {
    return new ResonatorSlot(owner === void 0 ? this.owner : owner, resonator === void 0 ? this.resonator : resonator, distance === void 0 ? this.distance : distance);
  };
  ResonatorSlot.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.owner) | 0;
    result = result * 31 + Kotlin.hashCode(this.resonator) | 0;
    result = result * 31 + Kotlin.hashCode(this.distance) | 0;
    return result;
  };
  ResonatorSlot.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.owner, other.owner) && Kotlin.equals(this.resonator, other.resonator) && Kotlin.equals(this.distance, other.distance)))));
  };
  function XmHeap(cores, isCollected) {
    XmHeap$Companion_getInstance();
    if (isCollected === void 0)
      isCollected = false;
    this.cores_0 = cores;
    this.isCollected_0 = isCollected;
    this.xm = this.cores_0.first + this.cores_0.second + this.cores_0.third | 0;
    this.IMAGE_0 = XmHeap$Companion_getInstance().drawHeapTemplate_0();
  }
  XmHeap.prototype.isCollected = function () {
    return this.isCollected_0;
  };
  XmHeap.prototype.collect = function () {
    this.isCollected_0 = true;
  };
  XmHeap.prototype.draw_lfj9be$ = function (position) {
    var image = this.IMAGE_0;
    var ww = image.width / 2 | 0;
    var hh = image.height / 2 | 0;
    World_getInstance().ctx().drawImage(image, position.x - ww, position.y - hh);
  };
  function XmHeap$Companion() {
    XmHeap$Companion_instance = this;
    this.coreCount_0 = 3;
    this.CORE_IMAGE_0 = this.drawCoreTemplate_0();
    this.minCapacity_0 = 35;
    this.maxCapacity_0 = 100;
    this.capacity = 65;
  }
  XmHeap$Companion.prototype.strayXmMinDistance_6taknv$ = function (isPortalDrop) {
    return isPortalDrop ? 13 : 21;
  };
  function XmHeap$Companion$drawHeapTemplate$lambda(closure$w, closure$h, closure$scatter, this$XmHeap$) {
    return function (ctx) {
      var $receiver = new IntRange(1, 3);
      var tmp$;
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        var closure$w_0 = closure$w;
        var closure$h_0 = closure$h;
        var closure$scatter_0 = closure$scatter;
        var this$XmHeap$_0 = this$XmHeap$;
        var p = Coords_init(closure$w_0 / 2 | 0, closure$h_0 / 2 | 0).randomNearPoint_za3lpa$(closure$scatter_0);
        ctx.drawImage(this$XmHeap$_0.CORE_IMAGE_0, p.x, p.y);
      }
    };
  }
  XmHeap$Companion.prototype.drawHeapTemplate_0 = function () {
    var r = 5;
    var scatter = 13;
    var w = 2 * (scatter + r + 1 | 0) | 0;
    var h = w;
    return HtmlUtil_getInstance().preRender_yb5akz$(w, h, XmHeap$Companion$drawHeapTemplate$lambda(w, h, scatter, this));
  };
  function XmHeap$Companion$drawCoreTemplate$lambda(closure$r, closure$stroke, closure$lineWidth, closure$fill, closure$alpha) {
    return function (ctx) {
      var circle = new Circle(Coords_init(closure$r, closure$r), closure$r);
      DrawUtil_getInstance().drawCircle_3kie0f$(ctx, circle, closure$stroke, closure$lineWidth, closure$fill, closure$alpha);
    };
  }
  XmHeap$Companion.prototype.drawCoreTemplate_0 = function () {
    var r = 2;
    var w = 4;
    var h = 4;
    var stroke = Colors_getInstance().white + '33';
    var lineWidth = 1.0;
    var fill = Colors_getInstance().white;
    var alpha = 0.4;
    return HtmlUtil_getInstance().preRender_yb5akz$(w, h, XmHeap$Companion$drawCoreTemplate$lambda(r, stroke, lineWidth, fill, alpha));
  };
  XmHeap$Companion.prototype.createCore_0 = function () {
    return Util_getInstance().randomInt_vux9f0$(35, 100);
  };
  XmHeap$Companion.prototype.createCores_0 = function () {
    return new Triple(this.createCore_0(), this.createCore_0(), this.createCore_0());
  };
  XmHeap$Companion.prototype.create = function () {
    return new XmHeap(this.createCores_0());
  };
  XmHeap$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var XmHeap$Companion_instance = null;
  function XmHeap$Companion_getInstance() {
    if (XmHeap$Companion_instance === null) {
      new XmHeap$Companion();
    }
    return XmHeap$Companion_instance;
  }
  XmHeap.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'XmHeap',
    interfaces: []
  };
  XmHeap.prototype.component1_0 = function () {
    return this.cores_0;
  };
  XmHeap.prototype.component2_0 = function () {
    return this.isCollected_0;
  };
  XmHeap.prototype.copy_g6oo13$ = function (cores, isCollected) {
    return new XmHeap(cores === void 0 ? this.cores_0 : cores, isCollected === void 0 ? this.isCollected_0 : isCollected);
  };
  XmHeap.prototype.toString = function () {
    return 'XmHeap(cores=' + Kotlin.toString(this.cores_0) + (', isCollected=' + Kotlin.toString(this.isCollected_0)) + ')';
  };
  XmHeap.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.cores_0) | 0;
    result = result * 31 + Kotlin.hashCode(this.isCollected_0) | 0;
    return result;
  };
  XmHeap.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.cores_0, other.cores_0) && Kotlin.equals(this.isCollected_0, other.isCollected_0)))));
  };
  function XmMap() {
    XmMap_instance = this;
    this.strayXm_0 = LinkedHashMap_init();
  }
  XmMap.prototype.updateStrayXm = function () {
    var $receiver = this.strayXm_0;
    var destination = LinkedHashMap_init();
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (!element.value.isCollected()) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    var unCollectedXm = destination;
    this.strayXm_0.clear();
    this.strayXm_0.putAll_a2k3zr$(unCollectedXm);
  };
  XmMap.prototype.createStrayXm_dusakv$ = function (location, isPortalDrop) {
    var $receiver = this.strayXm_0.keys;
    var none$result;
    none$break: do {
      var tmp$;
      if (Kotlin.isType($receiver, Collection) && $receiver.isEmpty()) {
        none$result = true;
        break none$break;
      }
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        if (element.distanceTo_lfj9be$(location) < XmHeap$Companion_getInstance().strayXmMinDistance_6taknv$(isPortalDrop)) {
          none$result = false;
          break none$break;
        }
      }
      none$result = true;
    }
     while (false);
    if (none$result) {
      var $receiver_0 = this.strayXm_0;
      var value = XmHeap$Companion_getInstance().create();
      $receiver_0.put_xwzc9p$(location, value);
    }
  };
  XmMap.prototype.findXmInRange_lfj9be$ = function (pos) {
    var $receiver = this.strayXm_0;
    var destination = LinkedHashMap_init();
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.key.distanceTo_lfj9be$(pos) <= Dim_getInstance().agentXmCollectionRadius) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    return destination;
  };
  XmMap.prototype.draw = function () {
    var tmp$;
    tmp$ = this.strayXm_0.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var pos = element.key;
      var heap = element.value;
      heap.draw_lfj9be$(pos);
    }
  };
  XmMap.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'XmMap',
    interfaces: []
  };
  var XmMap_instance = null;
  function XmMap_getInstance() {
    if (XmMap_instance === null) {
      new XmMap();
    }
    return XmMap_instance;
  }
  function Checkpoint(enlMu, resMu, isCycleEnd) {
    Checkpoint$Companion_getInstance();
    this.enlMu = enlMu;
    this.resMu = resMu;
    this.isCycleEnd = isCycleEnd;
  }
  Checkpoint.prototype.total = function () {
    return this.enlMu + this.resMu | 0;
  };
  Checkpoint.prototype.mu_bip15f$ = function (faction) {
    switch (faction.name) {
      case 'ENL':
        return this.enlMu;
      case 'RES':
        return this.resMu;
      case 'NONE':
        return 0;
      default:return Kotlin.noWhenBranchMatched();
    }
  };
  function Checkpoint$Companion() {
    Checkpoint$Companion_instance = this;
    this.durationH = 5;
  }
  Checkpoint$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Checkpoint$Companion_instance = null;
  function Checkpoint$Companion_getInstance() {
    if (Checkpoint$Companion_instance === null) {
      new Checkpoint$Companion();
    }
    return Checkpoint$Companion_instance;
  }
  Checkpoint.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Checkpoint',
    interfaces: []
  };
  Checkpoint.prototype.component1 = function () {
    return this.enlMu;
  };
  Checkpoint.prototype.component2 = function () {
    return this.resMu;
  };
  Checkpoint.prototype.component3 = function () {
    return this.isCycleEnd;
  };
  Checkpoint.prototype.copy_ydzd23$ = function (enlMu, resMu, isCycleEnd) {
    return new Checkpoint(enlMu === void 0 ? this.enlMu : enlMu, resMu === void 0 ? this.resMu : resMu, isCycleEnd === void 0 ? this.isCycleEnd : isCycleEnd);
  };
  Checkpoint.prototype.toString = function () {
    return 'Checkpoint(enlMu=' + Kotlin.toString(this.enlMu) + (', resMu=' + Kotlin.toString(this.resMu)) + (', isCycleEnd=' + Kotlin.toString(this.isCycleEnd)) + ')';
  };
  Checkpoint.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.enlMu) | 0;
    result = result * 31 + Kotlin.hashCode(this.resMu) | 0;
    result = result * 31 + Kotlin.hashCode(this.isCycleEnd) | 0;
    return result;
  };
  Checkpoint.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.enlMu, other.enlMu) && Kotlin.equals(this.resMu, other.resMu) && Kotlin.equals(this.isCycleEnd, other.isCycleEnd)))));
  };
  function Com() {
    Com_instance = this;
    this.messages_0 = ArrayList_init_0();
  }
  Com.prototype.addMessage_61zpoe$ = function (message) {
    this.messages_0.add_11rb$(message);
    if (this.messages_0.size > 8) {
      this.messages_0.removeAt_za3lpa$(0);
    }
  };
  Com.prototype.draw_f69bme$ = function (ctx) {
    var messages = toList_0(this.messages_0);
    var xPos = Dim_getInstance().width - Dim_getInstance().comRightOffset | 0;
    var yFixOffset = Dim_getInstance().height - Dim_getInstance().comBottomOffset | 0;
    var yOffset = (Dim_getInstance().comFontSize * 3 | 0) / 2 | 0;
    var reversed_0 = reversed(messages);
    var tmp$, tmp$_0;
    var index = 0;
    tmp$ = reversed_0.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      var pos = Coords_init(xPos, yFixOffset - Kotlin.imul(yOffset, checkIndexOverflow((tmp$_0 = index, index = tmp$_0 + 1 | 0, tmp$_0))) | 0);
      DrawUtil_getInstance().strokeText_lowmm9$(ctx, pos, item, Colors_getInstance().white, Dim_getInstance().comFontSize, DrawUtil_getInstance().CODA, 1.5);
    }
  };
  Com.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Com',
    interfaces: []
  };
  var Com_instance = null;
  function Com_getInstance() {
    if (Com_instance === null) {
      new Com();
    }
    return Com_instance;
  }
  function Cycle(name, ordinal, checkpoints, image) {
    Enum.call(this);
    this.checkpoints = checkpoints;
    this.image = image;
    this.name$ = name;
    this.ordinal$ = ordinal;
  }
  function Cycle_initFields() {
    Cycle_initFields = function () {
    };
    Cycle$INSTANCE_instance = new Cycle('INSTANCE', 0, LinkedHashMap_init(), null);
    Cycle$Companion_getInstance();
  }
  var Cycle$INSTANCE_instance;
  function Cycle$INSTANCE_getInstance() {
    Cycle_initFields();
    return Cycle$INSTANCE_instance;
  }
  function Cycle$Companion() {
    Cycle$Companion_instance = this;
    this.numberOfCheckpoints_0 = 35;
    this.ww = 8;
  }
  Cycle$Companion.prototype.isUpdateStuck_0 = function (tick) {
    return tick % 60 === 0;
  };
  Cycle$Companion.prototype.isNewCheckpoint_0 = function (tick) {
    return tick % Config_getInstance().ticksPerCheckpoint === 0;
  };
  Cycle$Companion.prototype.isNewCycle_0 = function (tick) {
    return tick % Config_getInstance().ticksPerCycle === 0;
  };
  function Cycle$Companion$updateCheckpoints$lambda(closure$tick) {
    return function (it) {
      return closure$tick;
    };
  }
  var compareBy$lambda_12 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_15(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_15.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_15.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Cycle$Companion.prototype.updateCheckpoints_qt1dr2$ = function (tick, enlMu, resMu) {
    if (this.isUpdateStuck_0(tick)) {
      var $receiver = World_getInstance().allAgents;
      var destination = ArrayList_init_0();
      var tmp$;
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        var tmp$_0;
        if ((tmp$_0 = element.action.item) != null ? tmp$_0.equals(ActionItem$Companion_getInstance().MOVE) : null)
          destination.add_11rb$(element);
      }
      var tmp$_1;
      tmp$_1 = destination.iterator();
      while (tmp$_1.hasNext()) {
        var element_0 = tmp$_1.next();
        element_0.updateLastPos();
      }
    }
    if (this.isNewCheckpoint_0(tick)) {
      var cp = new Checkpoint(enlMu, resMu, this.isNewCycle_0(tick));
      var limit = 34;
      var old = takeLast(sortedWith(toList(Cycle$INSTANCE_getInstance().checkpoints), new Comparator$ObjectLiteral_15(compareBy$lambda_12(Cycle$Companion$updateCheckpoints$lambda(tick)))), limit);
      Cycle$INSTANCE_getInstance().checkpoints.clear();
      putAll(Cycle$INSTANCE_getInstance().checkpoints, old);
      Cycle$INSTANCE_getInstance().checkpoints.put_xwzc9p$(tick, cp);
      Cycle$INSTANCE_getInstance().image = this.createImage_0();
      if (cp.isCycleEnd) {
        this.spawnXm_0();
        this.removePortals_0();
        this.removeFrogs_0();
        this.removeSmurfs_0();
        this.factionChange_0();
        SoundUtil_getInstance().playCycleSound();
        var tmp$_2;
        tmp$_2 = World_getInstance().allPortals.iterator();
        while (tmp$_2.hasNext()) {
          var element_1 = tmp$_2.next();
          element_1.decay();
        }
      }
       else {
        SoundUtil_getInstance().playCheckpointSound_2xtf47$(cp);
      }
    }
  };
  Cycle$Companion.prototype.removePortals_0 = function () {
    if (Util_getInstance().random() <= Config_getInstance().portalRemovalRate) {
      var ratio = World_getInstance().countPortals() / 89 | 0;
      if (Util_getInstance().random() <= ratio) {
        var deprecated = World_getInstance().randomPortal();
        deprecated.remove();
        Com_getInstance().addMessage_61zpoe$('Portal ' + deprecated + ' no longer exists.');
      }
    }
  };
  function Cycle$Companion$removeAgents$lambda(it) {
    return it.getLevel();
  }
  var compareBy$lambda_13 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_16(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_16.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_16.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Cycle$Companion.prototype.removeAgents_0 = function (agents, minCount, maxCount, fc) {
    if (fc === void 0)
      fc = false;
    var count = agents.size;
    if (count < minCount) {
      var ratio = count / maxCount | 0;
      if (Util_getInstance().random() <= ratio) {
        var selection = takeLast(sortedWith(agents, new Comparator$ObjectLiteral_16(compareBy$lambda_13(Cycle$Companion$removeAgents$lambda))), count - maxCount | 0);
        var removed = first(shuffled(selection));
        if (fc) {
          Com_getInstance().addMessage_61zpoe$('Portal ' + removed + ' quit the game.');
        }
         else {
          Com_getInstance().addMessage_61zpoe$('Portal ' + removed + ' has left ' + removed.faction.abbr + '.');
        }
      }
    }
  };
  Cycle$Companion.prototype.removeFrogs_0 = function () {
    if (Util_getInstance().random() <= Config_getInstance().frogQuitRate) {
      this.removeAgents_0(World_getInstance().frogs, 2, 21);
    }
  };
  Cycle$Companion.prototype.removeSmurfs_0 = function () {
    if (Util_getInstance().random() <= Config_getInstance().smurfQuitRate) {
      this.removeAgents_0(World_getInstance().smurfs, 2, 21);
    }
  };
  Cycle$Companion.prototype.factionChange_0 = function () {
    var tmp$;
    if (Util_getInstance().random() <= Config_getInstance().factionChangeRate) {
      if (Util_getInstance().randomBool()) {
        this.removeAgents_0(World_getInstance().frogs, 2, 21, true);
        tmp$ = Agent$Companion_getInstance().createSmurf_5edep5$(World_getInstance().grid);
      }
       else {
        this.removeAgents_0(World_getInstance().smurfs, 2, 21, true);
        tmp$ = Agent$Companion_getInstance().createFrog_5edep5$(World_getInstance().grid);
      }
      var xfAgent = tmp$;
      Com_getInstance().addMessage_61zpoe$(xfAgent.name + ' restarted as ' + xfAgent.faction.abbr + '.');
      World_getInstance().allAgents.add_11rb$(xfAgent);
    }
  };
  Cycle$Companion.prototype.spawnXm_0 = function () {
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(item.leakXm());
    }
    var destination_0 = ArrayList_init_0();
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var element = tmp$_0.next();
      var pos = element.component1()
      , xm = element.component2();
      var heapCount = xm / 65 | 0;
      var $receiver_0 = new IntRange(0, heapCount);
      var destination_1 = ArrayList_init(collectionSizeOrDefault($receiver_0, 10));
      var tmp$_1;
      tmp$_1 = $receiver_0.iterator();
      while (tmp$_1.hasNext()) {
        var item_0 = tmp$_1.next();
        destination_1.add_11rb$(pos.randomNearPoint_za3lpa$(Dim_getInstance().portalXmSpawnRadius));
      }
      var list = destination_1;
      addAll(destination_0, list);
    }
    var tmp$_2;
    tmp$_2 = destination_0.iterator();
    while (tmp$_2.hasNext()) {
      var element_0 = tmp$_2.next();
      XmMap_getInstance().createStrayXm_dusakv$(element_0, true);
    }
    var $receiver_1 = World_getInstance().allNonFaction;
    var destination_2 = ArrayList_init_0();
    var tmp$_3;
    tmp$_3 = $receiver_1.iterator();
    while (tmp$_3.hasNext()) {
      var element_1 = tmp$_3.next();
      if (!element_1.pos.isOffScreen())
        destination_2.add_11rb$(element_1);
    }
    var $receiver_2 = take(shuffled(destination_2), numberToInt(World_getInstance().allNonFaction.size * Config_getInstance().npcXmSpawnRatio));
    var destination_3 = ArrayList_init(collectionSizeOrDefault($receiver_2, 10));
    var tmp$_4;
    tmp$_4 = $receiver_2.iterator();
    while (tmp$_4.hasNext()) {
      var item_1 = tmp$_4.next();
      var tmp$_5 = destination_3.add_11rb$;
      XmMap_getInstance().createStrayXm_dusakv$(item_1.pos.randomNearPoint_za3lpa$(Dim_getInstance().npcXmSpawnRadius), false);
      tmp$_5.call(destination_3, Unit);
    }
  };
  function Cycle$Companion$createImage$drawCheckpointDot(closure$r, closure$lineWidth, closure$dotAlpha) {
    return function (ctx, pos, style, isCycleEnded) {
      var radius = isCycleEnded ? closure$r + 1 : closure$r;
      var circle = new Circle(pos, radius);
      DrawUtil_getInstance().drawCircle_3kie0f$(ctx, circle, Colors_getInstance().black, closure$lineWidth, style, closure$dotAlpha);
    };
  }
  function Cycle$Companion$createImage$drawCheckpoint$calcY(mu, maxTotal) {
    return Dim_getInstance().cycleH - (Kotlin.imul(mu, Dim_getInstance().cycleH) / maxTotal | 0) | 0;
  }
  function Cycle$Companion$createImage$drawCheckpoint(this$Cycle$, closure$r, closure$h, closure$lineWidth, closure$lineAlpha, closure$drawCheckpointDot) {
    return function (ctx, index, withNext, maxTotal) {
      var calcY = Cycle$Companion$createImage$drawCheckpoint$calcY;
      var x = Kotlin.imul(index, this$Cycle$.ww);
      var $receiver = Faction$Companion_getInstance().all();
      var tmp$;
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        var closure$r_0 = closure$r;
        var this$Cycle$_0 = this$Cycle$;
        var closure$h_0 = closure$h;
        var closure$lineWidth_0 = closure$lineWidth;
        var closure$lineAlpha_0 = closure$lineAlpha;
        var closure$drawCheckpointDot_0 = closure$drawCheckpointDot;
        var y = calcY(withNext.first.mu_bip15f$(element), maxTotal);
        var current = Coords_init(x, y + numberToInt(closure$r_0) + 2 | 0);
        var nextY = calcY(withNext.second.mu_bip15f$(element), maxTotal);
        var next = Coords_init(x + this$Cycle$_0.ww | 0, nextY + numberToInt(closure$r_0) + 2 | 0);
        var top = Coords_init(x + this$Cycle$_0.ww | 0, 0);
        var bot = Coords_init(x + this$Cycle$_0.ww | 0, closure$h_0 - 3 | 0);
        var lw = withNext.second.isCycleEnd ? 2.0 : 0.3;
        DrawUtil_getInstance().drawLine_ovbgws$(ctx, new Line(top, bot), Colors_getInstance().white, lw, 0.3);
        if (index > 0) {
          DrawUtil_getInstance().drawLine_ovbgws$(ctx, new Line(current, next), element.color, closure$lineWidth_0, closure$lineAlpha_0);
        }
        closure$drawCheckpointDot_0(ctx, next, element.color, withNext.second.isCycleEnd);
      }
    };
  }
  function Cycle$Companion$createImage$drawBackground(closure$h, closure$w) {
    return function (ctx) {
      DrawUtil_getInstance().drawRect_dve0j6$(ctx, Coords_init(0, 0), -closure$h, closure$w - 8, '#00000077', '#00000077', 0.0);
    };
  }
  function Cycle$Companion$createImage$drawBaseLine(closure$h, closure$off, closure$w) {
    return function (ctx) {
      var y = closure$h - closure$off | 0;
      var from = Coords_init(closure$off, y);
      var to = Coords_init(closure$w - closure$off - 8 | 0, y);
      DrawUtil_getInstance().drawLine_ovbgws$(ctx, new Line(from, to), Colors_getInstance().white, 2.0, 0.3);
    };
  }
  function Cycle$Companion$createImage$lambda(closure$drawBackground, closure$drawBaseLine, closure$drawCheckpoint) {
    return function (ctx) {
      var tmp$, tmp$_0;
      var checkpoints = Cycle$INSTANCE_getInstance().checkpoints;
      var $receiver = checkpoints.values;
      var maxBy$result;
      maxBy$break: do {
        var iterator = $receiver.iterator();
        if (!iterator.hasNext()) {
          maxBy$result = null;
          break maxBy$break;
        }
        var maxElem = iterator.next();
        var maxValue = maxElem.total();
        while (iterator.hasNext()) {
          var e = iterator.next();
          var v = e.total();
          if (Kotlin.compareTo(maxValue, v) < 0) {
            maxElem = e;
            maxValue = v;
          }
        }
        maxBy$result = maxElem;
      }
       while (false);
      var maxTotal = (tmp$_0 = (tmp$ = maxBy$result) != null ? tmp$.total() : null) != null ? tmp$_0 : 0;
      closure$drawBackground(ctx);
      closure$drawBaseLine(ctx);
      var $receiver_0 = zipWithNext(checkpoints.values);
      var destination = ArrayList_init(collectionSizeOrDefault($receiver_0, 10));
      var tmp$_1, tmp$_0_0;
      var index = 0;
      tmp$_1 = $receiver_0.iterator();
      while (tmp$_1.hasNext()) {
        var item = tmp$_1.next();
        var tmp$_2 = destination.add_11rb$;
        closure$drawCheckpoint(ctx, checkIndexOverflow((tmp$_0_0 = index, index = tmp$_0_0 + 1 | 0, tmp$_0_0)), item, maxTotal);
        tmp$_2.call(destination, Unit);
      }
    };
  }
  Cycle$Companion.prototype.createImage_0 = function () {
    var off = 4;
    var h = Dim_getInstance().cycleH + (2 * off | 0) | 0;
    var w = (this.ww * 35 | 0) - 1 + (2 * off | 0) | 0;
    var lineAlpha = 0.5;
    var dotAlpha = 0.5;
    var lineWidth = 1.0;
    var r = 2.0;
    var drawCheckpointDot = Cycle$Companion$createImage$drawCheckpointDot(r, lineWidth, dotAlpha);
    var drawCheckpoint = Cycle$Companion$createImage$drawCheckpoint(this, r, h, lineWidth, lineAlpha, drawCheckpointDot);
    var drawBackground = Cycle$Companion$createImage$drawBackground(h, w);
    var drawBaseLine = Cycle$Companion$createImage$drawBaseLine(h, off, w);
    return HtmlUtil_getInstance().preRender_yb5akz$(w, h, Cycle$Companion$createImage$lambda(drawBackground, drawBaseLine, drawCheckpoint));
  };
  Cycle$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Cycle$Companion_instance = null;
  function Cycle$Companion_getInstance() {
    Cycle_initFields();
    if (Cycle$Companion_instance === null) {
      new Cycle$Companion();
    }
    return Cycle$Companion_instance;
  }
  Cycle.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Cycle',
    interfaces: [Enum]
  };
  function Cycle$values() {
    return [Cycle$INSTANCE_getInstance()];
  }
  Cycle.values = Cycle$values;
  function Cycle$valueOf(name) {
    switch (name) {
      case 'INSTANCE':
        return Cycle$INSTANCE_getInstance();
      default:throwISE('No enum constant system.Cycle.' + name);
    }
  }
  Cycle.valueOf_61zpoe$ = Cycle$valueOf;
  function Attacks() {
    Attacks_instance = this;
    var $receiver = XmpLevel$values();
    var destination = ArrayList_init_0();
    var tmp$;
    for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
      var element = $receiver[tmp$];
      var $receiver_0 = new IntRange(0, Queues_getInstance().attackDelayTicks);
      var destination_0 = ArrayList_init(collectionSizeOrDefault($receiver_0, 10));
      var tmp$_0;
      tmp$_0 = $receiver_0.iterator();
      while (tmp$_0.hasNext()) {
        var item = tmp$_0.next();
        destination_0.add_11rb$(to(to(element, item), this.createDamageCircleImage_0(element, item)));
      }
      var list = destination_0;
      addAll(destination, list);
    }
    this.damageCircleImages = toMap(destination);
    var $receiver_1 = new IntRange(0, 100);
    var destination_1 = ArrayList_init(collectionSizeOrDefault($receiver_1, 10));
    var tmp$_1;
    tmp$_1 = $receiver_1.iterator();
    while (tmp$_1.hasNext()) {
      var item_0 = tmp$_1.next();
      destination_1.add_11rb$(to(item_0, this.createDamageImage_0(item_0, false)));
    }
    this.damageImages_0 = toMap(destination_1);
    var $receiver_2 = new IntRange(0, 100);
    var destination_2 = ArrayList_init(collectionSizeOrDefault($receiver_2, 10));
    var tmp$_2;
    tmp$_2 = $receiver_2.iterator();
    while (tmp$_2.hasNext()) {
      var item_1 = tmp$_2.next();
      destination_2.add_11rb$(to(item_1, this.createDamageImage_0(item_1, true)));
    }
    this.critDamageImages_0 = toMap(destination_2);
  }
  Attacks.prototype.draw = function () {
    var attackQueue = Queues_getInstance().attackQueue;
    var tmp$;
    tmp$ = attackQueue.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var futureTick = element.key;
      var ticksInFuture = futureTick - World_getInstance().tick | 0;
      var attackMap = element.value;
      var tmp$_0;
      tmp$_0 = attackMap.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        var pos = element_0.key;
        var bursters = element_0.value;
        var tmp$_1;
        tmp$_1 = bursters.iterator();
        while (tmp$_1.hasNext()) {
          var element_1 = tmp$_1.next();
          var image = this.damageCircleImages.get_11rb$(to(element_1.level, ticksInFuture));
          if (image != null) {
            World_getInstance().ctx().drawImage(image, pos.x - (image.width / 2 | 0), pos.y - (image.height / 2 | 0));
          }
        }
      }
    }
    var r = numberToInt(Dim_getInstance().maxDeploymentRange);
    var damageQueue = Queues_getInstance().damageQueue;
    var tmp$_2;
    tmp$_2 = damageQueue.entries.iterator();
    while (tmp$_2.hasNext()) {
      var element_2 = tmp$_2.next();
      var futureTick_0 = element_2.key;
      var ticksInFuture_0 = futureTick_0 - World_getInstance().tick | 0;
      var ratio = (Queues_getInstance().damageDelayTicks - ticksInFuture_0 | 0) / Queues_getInstance().damageDelayTicks | 0;
      var damageList = element_2.value;
      var tmp$_3;
      tmp$_3 = damageList.iterator();
      while (tmp$_3.hasNext()) {
        var element_3 = tmp$_3.next();
        var pos_0 = element_3.pos;
        var lineWidth = 3;
        var newPos = pos_0.copy_lu1900$(pos_0.x - r + lineWidth, pos_0.y - ratio - lineWidth);
        var image_0 = this.getImage_l68dqx$(element_3);
        World_getInstance().ctx().drawImage(image_0, newPos.x, newPos.y);
      }
    }
    Queues_getInstance().endTick_za3lpa$(World_getInstance().tick);
  };
  Attacks.prototype.getImage_l68dqx$ = function (damage) {
    var tmp$;
    var a = damage.value;
    var damagePercent = Math_0.min(a, 100);
    if (damage.isCritical) {
      tmp$ = getValue(this.critDamageImages_0, damagePercent);
    }
     else {
      tmp$ = getValue(this.damageImages_0, damagePercent);
    }
    return tmp$;
  };
  function Attacks$createDamageImage$lambda(closure$lineWidth, closure$fontSize, closure$damageValue, closure$isCritical) {
    return function (ctx) {
      var coords = Coords_init(numberToInt(closure$lineWidth) + ((closure$fontSize * 3 | 0) / 2 | 0) | 0, numberToInt(closure$lineWidth) + (closure$fontSize / 2 | 0) | 0);
      var a = closure$damageValue;
      var clipped = Math_0.max(a, 1).toString();
      var color = closure$isCritical ? Colors_getInstance().critDamage : Colors_getInstance().damage;
      var text = '-' + clipped + '%';
      DrawUtil_getInstance().strokeText_lowmm9$(ctx, coords, text, Colors_getInstance().white, closure$fontSize, DrawUtil_getInstance().CODA, closure$lineWidth, color);
    };
  }
  Attacks.prototype.createDamageImage_0 = function (damageValue, isCritical) {
    var fontSize = 11;
    var lineWidth = 3.0;
    var w = (fontSize * 5 | 0) + 2 * lineWidth;
    var h = fontSize + 2 * lineWidth;
    return HtmlUtil_getInstance().preRender_yb5akz$(numberToInt(w), numberToInt(h), Attacks$createDamageImage$lambda(lineWidth, fontSize, damageValue, isCritical));
  };
  function Attacks$createDamageCircleImage$lambda(closure$r, closure$lw, closure$strokeStyle, closure$fillStyle) {
    return function (ctx) {
      var attackCircle = new Circle(Coords_init(closure$r + closure$lw | 0, closure$r + closure$lw | 0), closure$r);
      DrawUtil_getInstance().drawCircle_3kie0f$(ctx, attackCircle, closure$strokeStyle, closure$lw, closure$fillStyle);
    };
  }
  Attacks.prototype.createDamageCircleImage_0 = function (xmpLevel, ticksInFuture) {
    var strokeStyle = '#ff731533';
    var fillStyle = '#fece5a11';
    var lw = 8;
    var ratio = (Queues_getInstance().damageDelayTicks - ticksInFuture | 0) / Queues_getInstance().damageDelayTicks | 0;
    var r = numberToInt(xmpLevel.rangeM * Dim_getInstance().pixelToMFactor * ratio);
    var w = (r * 2 | 0) + (2 * lw | 0) | 0;
    var h = w;
    return HtmlUtil_getInstance().preRender_yb5akz$(w, h, Attacks$createDamageCircleImage$lambda(r, lw, strokeStyle, fillStyle));
  };
  Attacks.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Attacks',
    interfaces: [Display]
  };
  var Attacks_instance = null;
  function Attacks_getInstance() {
    if (Attacks_instance === null) {
      new Attacks();
    }
    return Attacks_instance;
  }
  function Display() {
  }
  Display.$metadata$ = {
    kind: Kind_INTERFACE,
    simpleName: 'Display',
    interfaces: []
  };
  function Loading() {
    Loading$Companion_getInstance();
    this.lineWidth = 1.0;
    this.stroke = Colors_getInstance().black;
    this.fillOn = Colors_getInstance().white;
    this.fillOff = Colors_getInstance().white + '44';
  }
  function Loading$Companion() {
    Loading$Companion_instance = this;
  }
  Loading$Companion.prototype.clearUiLine_lu1900$ = function (y, h) {
    World_getInstance().uiCtx().clearRect(0.0, y, World_getInstance().uiCan.width, h);
  };
  Loading$Companion.prototype.draw = function () {
    var vecCount = World_getInstance().countPortals() + NonFaction$Companion_getInstance().offscreenCount() | 0;
    var vecY = 2.0 + 34.0 + Dim_getInstance().height / 2.0;
    var vecX = Dim_getInstance().width / 2.0 - Dim_getInstance().loadingBarLength / 2.0;
    var vecTot = 5 + NonFaction$Companion_getInstance().offscreenTotal() | 0;
    var vecH = 21.0;
    var npcY = vecY + vecH - 13.0;
    var npcH = 8.0;
    this.clearUiLine_lu1900$(vecY - vecH - 1, vecH + npcH + 2);
    VectorBar_getInstance().draw_61h0v2$(vecX, vecY, vecH, vecCount, vecTot);
    NpcBar_getInstance().draw_61h0v2$(vecX, npcY, npcH, World_getInstance().countNonFaction(), Config_getInstance().maxFor_bip15f$(Faction$NONE_getInstance()));
  };
  Loading$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Loading$Companion_instance = null;
  function Loading$Companion_getInstance() {
    if (Loading$Companion_instance === null) {
      new Loading$Companion();
    }
    return Loading$Companion_instance;
  }
  Loading.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Loading',
    interfaces: []
  };
  function LoadingText() {
    LoadingText_instance = this;
    Loading.call(this);
  }
  LoadingText.prototype.draw_61zpoe$ = function (text) {
    var y = (Dim_getInstance().height / 2 | 0) - 3 | 0;
    var x = numberToInt(Dim_getInstance().width / 2.0 - Dim_getInstance().loadingBarLength / 2.0) + 13 | 0;
    var lineWidth = 3.0;
    var strokeStyle = Colors_getInstance().black;
    var h = 21;
    var hh = h / 2 | 0;
    Loading$Companion_getInstance().clearUiLine_lu1900$(y - hh - 1, h + 2);
    DrawUtil_getInstance().strokeText_lowmm9$(World_getInstance().uiCtx(), Coords_init(x, y), text, Colors_getInstance().white, h, DrawUtil_getInstance().AMARILLO, lineWidth, strokeStyle);
  };
  LoadingText.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'LoadingText',
    interfaces: [Loading]
  };
  var LoadingText_instance = null;
  function LoadingText_getInstance() {
    if (LoadingText_instance === null) {
      new LoadingText();
    }
    return LoadingText_instance;
  }
  function NpcBar() {
    NpcBar_instance = this;
    Loading.call(this);
  }
  function NpcBar$draw$drawBack(closure$x, closure$y, closure$h, this$NpcBar) {
    return function () {
      var w = Dim_getInstance().loadingBarLength;
      DrawUtil_getInstance().drawExactRect_nmgd9k$(World_getInstance().uiCtx(), closure$x, closure$y, closure$h, w, this$NpcBar.fillOff, this$NpcBar.stroke, this$NpcBar.lineWidth);
    };
  }
  function NpcBar$draw$drawValue(closure$value, closure$of, closure$x, closure$y, closure$h, this$NpcBar) {
    return function () {
      var w = Dim_getInstance().loadingBarLength * closure$value / closure$of;
      DrawUtil_getInstance().drawExactRect_nmgd9k$(World_getInstance().uiCtx(), closure$x, closure$y, closure$h, w, this$NpcBar.fillOn, this$NpcBar.stroke, this$NpcBar.lineWidth);
    };
  }
  NpcBar.prototype.draw_61h0v2$ = function (x, y, h, value, of) {
    var drawBack = NpcBar$draw$drawBack(x, y, h, this);
    var drawValue = NpcBar$draw$drawValue(value, of, x, y, h, this);
    drawBack();
    drawValue();
  };
  NpcBar.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'NpcBar',
    interfaces: [Loading]
  };
  var NpcBar_instance = null;
  function NpcBar_getInstance() {
    if (NpcBar_instance === null) {
      new NpcBar();
    }
    return NpcBar_instance;
  }
  function VectorBar() {
    VectorBar_instance = this;
    Loading.call(this);
  }
  VectorBar.prototype.draw_61h0v2$ = function (x, y, h, value, of) {
    var w = Dim_getInstance().loadingBarLength / of;
    var tmp$;
    tmp$ = until(0, of).iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var xx = x + element * w;
      var fill = element <= value ? this.fillOn : this.fillOff;
      DrawUtil_getInstance().drawExactRect_nmgd9k$(World_getInstance().uiCtx(), xx, y, h, w, fill, this.stroke, this.lineWidth);
    }
  };
  VectorBar.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'VectorBar',
    interfaces: [Loading]
  };
  var VectorBar_instance = null;
  function VectorBar_getInstance() {
    if (VectorBar_instance === null) {
      new VectorBar();
    }
    return VectorBar_instance;
  }
  function ActionLimitsDisplay() {
    ActionLimitsDisplay_instance = this;
  }
  ActionLimitsDisplay.prototype.topArea_0 = function () {
    return Line$Companion_getInstance().create_tjonv8$(0, 0, Dim_getInstance().width, HtmlUtil_getInstance().topActionOffset());
  };
  ActionLimitsDisplay.prototype.bottomArea_0 = function () {
    return Line$Companion_getInstance().create_tjonv8$(0, Dim_getInstance().height - numberToInt(Dim_getInstance().botActionOffset) | 0, Dim_getInstance().width, Dim_getInstance().height);
  };
  ActionLimitsDisplay.prototype.leftSliderMouseArea_0 = function () {
    return Line$Companion_getInstance().create_tjonv8$(0, HtmlUtil_getInstance().topActionOffset(), HtmlUtil_getInstance().leftSliderWidth(), HtmlUtil_getInstance().topActionOffset() + HtmlUtil_getInstance().leftSliderHeight() | 0);
  };
  ActionLimitsDisplay.prototype.rightSliderMouseArea_0 = function () {
    return Line$Companion_getInstance().create_tjonv8$(Dim_getInstance().width - HtmlUtil_getInstance().rightSliderWidth() | 0, HtmlUtil_getInstance().topActionOffset(), Dim_getInstance().width, HtmlUtil_getInstance().topActionOffset() + HtmlUtil_getInstance().rightSliderHeight() | 0);
  };
  ActionLimitsDisplay.prototype.leftSliderArea_0 = function () {
    return Line$Companion_getInstance().create_tjonv8$(0, HtmlUtil_getInstance().topActionOffset(), HtmlUtil_getInstance().leftSliderWidth(), HtmlUtil_getInstance().leftSliderHeight());
  };
  ActionLimitsDisplay.prototype.rightSliderArea_0 = function () {
    return Line$Companion_getInstance().create_tjonv8$(Dim_getInstance().width - HtmlUtil_getInstance().rightSliderWidth() | 0, HtmlUtil_getInstance().topActionOffset(), Dim_getInstance().width, HtmlUtil_getInstance().rightSliderHeight());
  };
  ActionLimitsDisplay.prototype.blockedAreas_0 = function () {
    return listOf([this.topArea_0(), this.bottomArea_0(), this.leftSliderMouseArea_0(), this.rightSliderMouseArea_0()]);
  };
  ActionLimitsDisplay.prototype.isBlocked_lfj9be$ = function (pos) {
    var $receiver = this.blockedAreas_0();
    var any$result;
    any$break: do {
      var tmp$;
      if (Kotlin.isType($receiver, Collection) && $receiver.isEmpty()) {
        any$result = false;
        break any$break;
      }
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        if (element.isPointInArea_lfj9be$(pos)) {
          any$result = true;
          break any$break;
        }
      }
      any$result = false;
    }
     while (false);
    return any$result;
  };
  ActionLimitsDisplay.prototype.isNotBlocked_lfj9be$ = function (pos) {
    var $receiver = this.blockedAreas_0();
    var none$result;
    none$break: do {
      var tmp$;
      if (Kotlin.isType($receiver, Collection) && $receiver.isEmpty()) {
        none$result = true;
        break none$break;
      }
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        if (element.isPointInArea_lfj9be$(pos)) {
          none$result = false;
          break none$break;
        }
      }
      none$result = true;
    }
     while (false);
    return none$result;
  };
  ActionLimitsDisplay.prototype.draw = function () {
    this.drawArea_0(this.topArea_0());
    this.drawArea_0(this.bottomArea_0());
    this.drawArea_0(this.leftSliderArea_0());
    this.drawArea_0(this.rightSliderArea_0());
  };
  ActionLimitsDisplay.prototype.drawTop = function () {
    this.drawArea_0(this.topArea_0());
  };
  ActionLimitsDisplay.prototype.drawArea_0 = function (area) {
    var $receiver = World_getInstance().ctx();
    $receiver.beginPath();
    $receiver.fillStyle = '#00000077';
    this.fillArea_0(area);
    $receiver.closePath();
  };
  ActionLimitsDisplay.prototype.fillArea_0 = function (line) {
    if (line.isValidArea()) {
      World_getInstance().ctx().fillRect(line.fromX, line.fromY, line.toX, line.toY);
    }
  };
  ActionLimitsDisplay.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'ActionLimitsDisplay',
    interfaces: [Display]
  };
  var ActionLimitsDisplay_instance = null;
  function ActionLimitsDisplay_getInstance() {
    if (ActionLimitsDisplay_instance === null) {
      new ActionLimitsDisplay();
    }
    return ActionLimitsDisplay_instance;
  }
  function CycleDisplay() {
    CycleDisplay_instance = this;
  }
  CycleDisplay.prototype.draw = function () {
    if (Cycle$INSTANCE_getInstance().image != null) {
      var xPos = Dim_getInstance().width - Dim_getInstance().cycleRightOffset | 0;
      var yPos = Dim_getInstance().cycleTopOffset;
      World_getInstance().uiCtx().drawImage(Cycle$INSTANCE_getInstance().image, xPos, yPos);
    }
  };
  CycleDisplay.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'CycleDisplay',
    interfaces: [Display]
  };
  var CycleDisplay_instance = null;
  function CycleDisplay_getInstance() {
    if (CycleDisplay_instance === null) {
      new CycleDisplay();
    }
    return CycleDisplay_instance;
  }
  function MindUnits() {
    MindUnits_instance = this;
  }
  function MindUnits$draw$fillMuRect(from, width, height, fill, stroke, line) {
    var $receiver = World_getInstance().uiCtx();
    if (Styles_getInstance().isFillMuDisplay) {
      $receiver.globalAlpha = 0.3;
      $receiver.fillStyle = fill;
      $receiver.fillRect(from.x, from.y, width, height);
    }
    $receiver.strokeStyle = stroke;
    $receiver.globalAlpha = 1.0;
    $receiver.lineWidth = line;
    $receiver.beginPath();
    $receiver.strokeRect(from.x, from.y, width, height);
    $receiver.closePath();
    $receiver.stroke();
  }
  function MindUnits$draw$drawMuRect(closure$fillMuRect) {
    return function (pos, part, faction, mu) {
      var fromRect = new Coords(pos.x, pos.y - Dim_getInstance().muFontSize);
      var width = 1.5 * part;
      var height = Dim_getInstance().muFontSize * Constants_getInstance().phi;
      closure$fillMuRect(fromRect, width, height, faction.color, faction.color, 3.0);
      var text = faction.abbr + ' ' + toString(mu) + 'M';
      var textPos = new Coords(pos.x + 21, pos.y - 3);
      DrawUtil_getInstance().strokeText_lowmm9$(World_getInstance().uiCtx(), textPos, text, faction.color, Dim_getInstance().muFontSize, DrawUtil_getInstance().AMARILLO);
    };
  }
  MindUnits.prototype.draw_yadwiv$ = function (firstMu, secondMu, factions) {
    var fillMuRect = MindUnits$draw$fillMuRect;
    var drawMuRect = MindUnits$draw$drawMuRect(fillMuRect);
    var totalMu = firstMu + secondMu | 0;
    var firstPart = numberToInt(round(100.0 * firstMu / totalMu));
    var secondPart = numberToInt(round(100.0 * secondMu / totalMu));
    var xPos = Dim_getInstance().muLeftOffset;
    var yPos = Dim_getInstance().height - Dim_getInstance().muBottomOffset | 0;
    var firstPos = Coords_init(xPos, yPos - (Dim_getInstance().muFontSize * 2 | 0) | 0);
    var secondPos = Coords_init(xPos, yPos);
    drawMuRect(firstPos, firstPart, factions.first, firstMu);
    drawMuRect(secondPos, secondPart, factions.second, secondMu);
  };
  MindUnits.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'MindUnits',
    interfaces: []
  };
  var MindUnits_instance = null;
  function MindUnits_getInstance() {
    if (MindUnits_instance === null) {
      new MindUnits();
    }
    return MindUnits_instance;
  }
  function StatsDisplay() {
    StatsDisplay_instance = this;
    this.fontSize_0 = 13;
    this.lineWidth_0 = 3.0;
  }
  function StatsDisplay$draw$drawCell(this$StatsDisplay) {
    return function (pos, text, color) {
      DrawUtil_getInstance().strokeText_lowmm9$(World_getInstance().uiCtx(), pos, text, color, 13, DrawUtil_getInstance().CODA, this$StatsDisplay.lineWidth_0, Colors_getInstance().black, 'end');
    };
  }
  function StatsDisplay$draw$drawRow(closure$yOff, closure$drawCell, closure$yStep) {
    return function (pos, header, factions, first, second, total) {
      closure$drawCell(Coords_init(pos, closure$yOff), header, Colors_getInstance().white);
      closure$drawCell(Coords_init(pos, closure$yOff + closure$yStep | 0), first.toString(), factions.first.color);
      closure$drawCell(Coords_init(pos, closure$yOff + (closure$yStep * 2 | 0) | 0), second.toString(), factions.second.color);
      closure$drawCell(Coords_init(pos, closure$yOff + (closure$yStep * 3 | 0) | 0), total.toString(), Colors_getInstance().white);
    };
  }
  StatsDisplay.prototype.draw = function () {
    var drawCell = StatsDisplay$draw$drawCell(this);
    var yOff = Dim_getInstance().statsTopOffset;
    var yStep = 19;
    var xStep = 55;
    var drawRow = StatsDisplay$draw$drawRow(yOff, drawCell, yStep);
    var xPos = Dim_getInstance().width - Dim_getInstance().statsRightOffset | 0;
    var factions = to(ensureNotNull(World_getInstance().userFaction), ensureNotNull(World_getInstance().userFaction).enemy());
    var $receiver = World_getInstance();
    var tmp$;
    tmp$ = (new IntRange(1, 4)).iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      switch (element) {
        case 1:
          drawRow(xPos, 'Agents', factions, $receiver.countAgents_bip15f$(factions.first), $receiver.countAgents_bip15f$(factions.second), $receiver.countAgents());
          break;
        case 2:
          drawRow(xPos + xStep | 0, 'Portals', factions, $receiver.countPortals_bip15f$(factions.first), $receiver.countPortals_bip15f$(factions.second), $receiver.countPortals());
          break;
        case 3:
          drawRow(xPos + (xStep * 2 | 0) | 0, 'Links', factions, $receiver.countLinks_bip15f$(factions.first), $receiver.countLinks_bip15f$(factions.second), $receiver.countLinks());
          break;
        case 4:
          drawRow(xPos + (xStep * 3 | 0) | 0, 'Fields', factions, $receiver.countFields_bip15f$(factions.first), $receiver.countFields_bip15f$(factions.second), $receiver.countFields());
          break;
      }
    }
    return Unit;
  };
  StatsDisplay.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'StatsDisplay',
    interfaces: [Display]
  };
  var StatsDisplay_instance = null;
  function StatsDisplay_getInstance() {
    if (StatsDisplay_instance === null) {
      new StatsDisplay();
    }
    return StatsDisplay_instance;
  }
  function TopAgentsDisplay() {
    TopAgentsDisplay_instance = this;
    UiTable.call(this);
  }
  function TopAgentsDisplay$draw$drawBars(closure$fontSize, closure$lineWidth) {
    return function (ctx, barWidth, level, color, pos, count, maxCount) {
      var xOffset = Kotlin.imul(barWidth, level) - barWidth | 0;
      var statPos = new Coords(pos.x + xOffset, pos.y + (closure$fontSize / 2 | 0));
      var h = closure$fontSize * count / maxCount;
      DrawUtil_getInstance().drawRect_dve0j6$(ctx, statPos, h, barWidth, color, Colors_getInstance().black, closure$lineWidth);
    };
  }
  var mapCapacity = Kotlin.kotlin.collections.mapCapacity_za3lpa$;
  var LinkedHashMap_init_0 = Kotlin.kotlin.collections.LinkedHashMap_init_bwtc7$;
  function TopAgentsDisplay$draw$drawCounts(closure$fontSize, closure$lineWidth, closure$drawBars) {
    return function (ctx, items, col, offset, isShields) {
      if (isShields === void 0)
        isShields = false;
      var tmp$;
      var pos = new Coords(col.x + offset, col.y);
      var barWidth = 6;
      var totalWidth = 48;
      var statPos = new Coords(pos.x, pos.y + (closure$fontSize / 2 | 0));
      DrawUtil_getInstance().drawRect_dve0j6$(ctx, statPos, 0.0, totalWidth, Colors_getInstance().black, Colors_getInstance().black, closure$lineWidth);
      if (items == null || items.isEmpty()) {
        DrawUtil_getInstance().strokeText_lowmm9$(ctx, pos, '0', Colors_getInstance().white, closure$fontSize, DrawUtil_getInstance().CODA, closure$lineWidth, Colors_getInstance().black, 'right');
      }
       else {
        DrawUtil_getInstance().strokeText_lowmm9$(ctx, pos, items.size.toString(), Colors_getInstance().white, closure$fontSize, DrawUtil_getInstance().CODA, closure$lineWidth, Colors_getInstance().black, 'right');
        var destination = LinkedHashMap_init();
        var tmp$_0;
        tmp$_0 = items.iterator();
        while (tmp$_0.hasNext()) {
          var element = tmp$_0.next();
          var key = element.getLevel();
          var tmp$_0_0;
          var value = destination.get_11rb$(key);
          if (value == null) {
            var answer = ArrayList_init_0();
            destination.put_xwzc9p$(key, answer);
            tmp$_0_0 = answer;
          }
           else {
            tmp$_0_0 = value;
          }
          var list = tmp$_0_0;
          list.add_11rb$(element);
        }
        var itemsByLevel = destination;
        var destination_0 = LinkedHashMap_init_0(mapCapacity(itemsByLevel.size));
        var tmp$_1;
        tmp$_1 = itemsByLevel.entries.iterator();
        while (tmp$_1.hasNext()) {
          var element_0 = tmp$_1.next();
          destination_0.put_xwzc9p$(element_0.key, element_0.value.size);
        }
        var countsByLevel = destination_0;
        var destination_1 = ArrayList_init(countsByLevel.size);
        var tmp$_2;
        tmp$_2 = countsByLevel.entries.iterator();
        while (tmp$_2.hasNext()) {
          var item = tmp$_2.next();
          destination_1.add_11rb$(item.value);
        }
        var maxCount = (tmp$ = max(destination_1)) != null ? tmp$ : 0;
        if (isShields) {
          var $receiver = new IntRange(1, 4);
          var tmp$_3;
          tmp$_3 = $receiver.iterator();
          while (tmp$_3.hasNext()) {
            var element_1 = tmp$_3.next();
            var closure$drawBars_0 = closure$drawBars;
            var tmp$_4;
            var count = (tmp$_4 = countsByLevel.get_11rb$(element_1)) != null ? tmp$_4 : 0;
            if (count > 0) {
              var color = ShieldType$Companion_getInstance().getColorForLevel_za3lpa$(element_1);
              closure$drawBars_0(ctx, barWidth * 2 | 0, element_1, color, pos, count, maxCount);
            }
          }
        }
         else {
          var $receiver_0 = new IntRange(1, 8);
          var tmp$_5;
          tmp$_5 = $receiver_0.iterator();
          while (tmp$_5.hasNext()) {
            var element_2 = tmp$_5.next();
            var closure$drawBars_1 = closure$drawBars;
            var tmp$_6, tmp$_7;
            var count_0 = (tmp$_6 = countsByLevel.get_11rb$(element_2)) != null ? tmp$_6 : 0;
            if (count_0 > 0) {
              var color_0 = (tmp$_7 = LevelColor_getInstance().map.get_11rb$(element_2)) != null ? tmp$_7 : '#FFFFFF';
              closure$drawBars_1(ctx, barWidth, element_2, color_0, pos, count_0, maxCount);
            }
          }
        }
      }
    };
  }
  function TopAgentsDisplay$draw$lambda(it) {
    return -it.ap | 0;
  }
  var compareBy$lambda_14 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_17(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_17.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_17.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  TopAgentsDisplay.prototype.draw = function () {
    var fontSize = Dim_getInstance().topAgentsInventoryFontSize;
    var lineWidth = 2.0;
    var drawBars = TopAgentsDisplay$draw$drawBars(fontSize, lineWidth);
    var drawCounts = TopAgentsDisplay$draw$drawCounts(fontSize, lineWidth, drawBars);
    var ctx = World_getInstance().uiCtx();
    ctx.globalAlpha = 1.0;
    var xPos = Dim_getInstance().topAgentsLeftOffset;
    var yOffset = (Dim_getInstance().topAgentsFontSize * 3 | 0) / 2 | 0;
    var yFixOffset = Dim_getInstance().height - Dim_getInstance().topAgentsBottomOffset - (8 * yOffset | 0) | 0;
    var headerPos = Coords_init(xPos, yFixOffset - yOffset | 0);
    var top = take(sortedWith(toList_0(World_getInstance().allAgents), new Comparator$ObjectLiteral_17(compareBy$lambda_14(TopAgentsDisplay$draw$lambda))), 8);
    var tmp$, tmp$_0;
    var index = 0;
    tmp$ = top.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      var index_0 = checkIndexOverflow((tmp$_0 = index, index = tmp$_0 + 1 | 0, tmp$_0));
      var rank = (index_0 + 1 | 0).toString();
      var name = item.toString();
      var pos = Coords_init(xPos, yFixOffset + Kotlin.imul(yOffset, index_0) | 0);
      var offset = 0;
      this.strokeTableText_7rqwur$(pos, offset, rank, 'right');
      offset = offset + 10 | 0;
      this.strokeTableHeaderText_8gukhi$(headerPos, offset, 'XM');
      this.strokeTableText_7rqwur$(pos, offset + 28 | 0, item.xm.toString(), 'right');
      offset = offset + 34 | 0;
      this.strokeTableHeaderText_8gukhi$(headerPos, offset, 'AP');
      this.strokeTableText_7rqwur$(pos, offset + 44 | 0, item.ap.toString(), 'right');
      offset = offset + 50 | 0;
      this.strokeTableHeaderText_8gukhi$(headerPos, offset, 'Agent');
      this.strokeTableText_7rqwur$(pos, offset, name, 'start', item.faction.color);
      offset = offset + 100 | 0;
      this.strokeTableHeaderText_8gukhi$(headerPos, offset, 'XMPs');
      drawCounts(ctx, item.inventory.findXmps(), pos, offset);
      offset = offset + 70 | 0;
      this.strokeTableHeaderText_8gukhi$(headerPos, offset, 'Resos');
      drawCounts(ctx, item.inventory.findResonators(), pos, offset);
      offset = offset + 70 | 0;
      this.strokeTableHeaderText_8gukhi$(headerPos, offset, 'Cubes');
      drawCounts(ctx, item.inventory.findPowerCubes(), pos, offset);
      offset = offset + 70 | 0;
      this.strokeTableHeaderText_8gukhi$(headerPos, offset, 'Shields');
      drawCounts(ctx, item.inventory.findShields(), pos, offset, true);
      offset = offset + 60 | 0;
      var keyCount = item.inventory.keyCount();
      this.strokeTableHeaderText_8gukhi$(headerPos, offset, 'Keys');
      this.strokeTableText_7rqwur$(pos, offset + 24 | 0, keyCount.toString(), 'right');
      offset = offset + 30 | 0;
      this.strokeTableHeaderText_8gukhi$(headerPos, offset, 'Action');
      var iconRadius = Dim_getInstance().agentRadius;
      var actionIconPos = new Coords(pos.x + offset - iconRadius, pos.y - iconRadius - 2);
      this.addIcon_4tdfr2$(actionIconPos, item.action.item);
      this.strokeTableText_7rqwur$(pos, offset + (iconRadius * 2 | 0) + 7 | 0, item.action.toString());
      offset = offset + 70 | 0;
      this.strokeTableHeaderText_8gukhi$(headerPos, offset, 'Portal');
      this.strokeTableText_7rqwur$(pos, offset, item.actionPortal.name);
    }
  };
  TopAgentsDisplay.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'TopAgentsDisplay',
    interfaces: [UiTable]
  };
  var TopAgentsDisplay_instance = null;
  function TopAgentsDisplay_getInstance() {
    if (TopAgentsDisplay_instance === null) {
      new TopAgentsDisplay();
    }
    return TopAgentsDisplay_instance;
  }
  function UiTable() {
  }
  UiTable.prototype.strokeTableHeaderText_8gukhi$ = function (headerPos, offset, text) {
    var pos = new Coords(headerPos.x + offset, headerPos.y);
    DrawUtil_getInstance().strokeText_lowmm9$(World_getInstance().uiCtx(), pos, text, Colors_getInstance().white, Dim_getInstance().topAgentsFontSize, DrawUtil_getInstance().CODA, 3.0);
  };
  UiTable.prototype.strokeTableText_7rqwur$ = function (headerPos, offset, text, textAlign, fillStyle) {
    if (textAlign === void 0) {
      textAlign = 'start';
    }
    if (fillStyle === void 0)
      fillStyle = Colors_getInstance().white;
    var pos = new Coords(headerPos.x + offset, headerPos.y);
    DrawUtil_getInstance().strokeText_lowmm9$(World_getInstance().uiCtx(), pos, text, fillStyle, Dim_getInstance().topAgentsFontSize, DrawUtil_getInstance().CODA, 3.0, Colors_getInstance().black, textAlign);
  };
  UiTable.prototype.addIcon_4tdfr2$ = function (pos, item) {
    var image = ActionItem$Companion_getInstance().getIcon_5bvev3$(item);
    World_getInstance().uiCtx().drawImage(image, pos.x, pos.y);
  };
  UiTable.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'UiTable',
    interfaces: []
  };
  function TickDisplay() {
    TickDisplay_instance = this;
  }
  TickDisplay.prototype.draw = function () {
    var pos = Coords_init(13, Dim_getInstance().height - Dim_getInstance().tickBottomOffset | 0);
    var half = Dim_getInstance().tickFontSize / 2 | 0;
    var $receiver = World_getInstance().uiCtx();
    $receiver.fillStyle = '#00000077';
    $receiver.fillRect(pos.x - 8, pos.y - half - 1, 164.0, Dim_getInstance().tickFontSize + 2.0);
    $receiver.fill();
    $receiver.globalAlpha = 1.0;
    var stamp = Time_getInstance().ticksToTimestamp_za3lpa$(World_getInstance().tick);
    DrawUtil_getInstance().drawText_omkwws$(World_getInstance().uiCtx(), pos, stamp, Colors_getInstance().white, Dim_getInstance().tickFontSize, DrawUtil_getInstance().CODA);
    var tick = ' Tick: ' + toString(World_getInstance().tick);
    DrawUtil_getInstance().drawText_omkwws$(World_getInstance().uiCtx(), pos.copy_lu1900$(pos.x + 55), tick, Colors_getInstance().white, Dim_getInstance().tickFontSize, DrawUtil_getInstance().CODA);
  };
  TickDisplay.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'TickDisplay',
    interfaces: [Display]
  };
  var TickDisplay_instance = null;
  function TickDisplay_getInstance() {
    if (TickDisplay_instance === null) {
      new TickDisplay();
    }
    return TickDisplay_instance;
  }
  function VectorFields() {
    VectorFields_instance = this;
    this.VECTORS_0 = LinkedHashMap_init();
  }
  VectorFields.prototype.draw_hv9zn6$ = function (portal) {
    this.draw_v4iyov$(portal.vectorField);
    portal.drawCenter_j4cg6b$(World_getInstance().bgCtx(), false);
  };
  function VectorFields$draw$lambda$isWalkable(closure$it) {
    return function () {
      var tmp$, tmp$_0;
      return (tmp$_0 = (tmp$ = World_getInstance().grid.get_11rb$(closure$it.key)) != null ? tmp$.isPassable : null) != null ? tmp$_0 : false;
    };
  }
  VectorFields.prototype.draw_v4iyov$ = function (vectorField) {
    if (World_getInstance().isReady)
      return;
    World_getInstance().bgCtx().clearRect(0.0, 0.0, Dim_getInstance().width, Dim_getInstance().height);
    var w = Coords$Companion_getInstance().res - 1 | 0;
    var h = Coords$Companion_getInstance().res - 1 | 0;
    var tmp$;
    tmp$ = vectorField.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var isWalkable = VectorFields$draw$lambda$isWalkable(element);
      if (Styles_getInstance().isDrawObstructedVectors || isWalkable()) {
        var vectorImageData = this.getOrCreateVectorImageData_0(w, h, element.value);
        var pos = element.key.fromShadow();
        if (!HtmlUtil_getInstance().isBlockedByMapbox_lfj9be$(pos)) {
          World_getInstance().bgCtx().putImageData(vectorImageData, pos.x, pos.y);
        }
      }
    }
  };
  function VectorFields$VecKey(line, style, isColor) {
    this.line = line;
    this.style = style;
    this.isColor = isColor;
  }
  VectorFields$VecKey.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'VecKey',
    interfaces: []
  };
  VectorFields$VecKey.prototype.component1 = function () {
    return this.line;
  };
  VectorFields$VecKey.prototype.component2 = function () {
    return this.style;
  };
  VectorFields$VecKey.prototype.component3 = function () {
    return this.isColor;
  };
  VectorFields$VecKey.prototype.copy_ctysrf$ = function (line, style, isColor) {
    return new VectorFields$VecKey(line === void 0 ? this.line : line, style === void 0 ? this.style : style, isColor === void 0 ? this.isColor : isColor);
  };
  VectorFields$VecKey.prototype.toString = function () {
    return 'VecKey(line=' + Kotlin.toString(this.line) + (', style=' + Kotlin.toString(this.style)) + (', isColor=' + Kotlin.toString(this.isColor)) + ')';
  };
  VectorFields$VecKey.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.line) | 0;
    result = result * 31 + Kotlin.hashCode(this.style) | 0;
    result = result * 31 + Kotlin.hashCode(this.isColor) | 0;
    return result;
  };
  VectorFields$VecKey.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.line, other.line) && Kotlin.equals(this.style, other.style) && Kotlin.equals(this.isColor, other.isColor)))));
  };
  VectorFields.prototype.findVec_0 = function (line, style, isColor) {
    var key = new VectorFields$VecKey(line, style, isColor);
    return this.VECTORS_0.get_11rb$(key);
  };
  VectorFields.prototype.putVec_0 = function (line, style, isColor, image) {
    var key = new VectorFields$VecKey(line, style, isColor);
    this.VECTORS_0.put_xwzc9p$(key, image);
  };
  VectorFields.prototype.createLine_0 = function (center, scaled) {
    var re = numberToInt(scaled.re);
    var im = numberToInt(scaled.im);
    var negRe = numberToInt(re / Constants_getInstance().phi);
    var negIm = numberToInt(im / Constants_getInstance().phi);
    var from = Coords_init(center - negRe | 0, center - negIm | 0);
    var to = Coords_init(center + re | 0, center + im | 0);
    return new Line(from, to);
  };
  VectorFields.prototype.getOrCreateVectorImageData_0 = function (w, h, complex) {
    var tmp$, tmp$_0;
    var style = Styles_getInstance().vectorStyle();
    var isColor = Styles_getInstance().isColorVectors();
    var center = Coords$Companion_getInstance().res / 2 | 0;
    var vecMag = center;
    var scaled = Complex$Companion_getInstance().fromMagnitudeAndPhase_lu1900$(vecMag, complex.phase);
    var line = this.createLine_0(center, scaled);
    var maybeImage = this.findVec_0(line, style, isColor);
    if (maybeImage != null) {
      tmp$_0 = maybeImage;
    }
     else {
      var newImageCan = this.createVectorImage_0(w, h, complex, line, style, isColor);
      var newImageCtx = Kotlin.isType(tmp$ = newImageCan.getContext('2d'), CanvasRenderingContext2D) ? tmp$ : throwCCE();
      var imageData = newImageCtx.getImageData(0, 0, w, h);
      this.putVec_0(line, style, isColor, imageData);
      tmp$_0 = imageData;
    }
    return tmp$_0;
  };
  VectorFields.prototype.drawCircle_0 = function (ctx, r) {
    var path = new Path2D();
    path.moveTo(r, r);
    path.arc(r, r, r, 0.0, 2.0 * math.PI);
    ctx.fill(path);
  };
  VectorFields.prototype.drawSquare_0 = function (ctx, w, h) {
    ctx.fillRect(1.0, 1.0, w, h);
    ctx.fill();
  };
  function VectorFields$createVectorImage$lambda(closure$style, closure$w, this$VectorFields, closure$h, closure$line, closure$stroke) {
    return function (ctx) {
      ctx.fillStyle = '#ffffff44';
      switch (closure$style.name) {
        case 'CIRCLE':
          this$VectorFields.drawCircle_0(ctx, closure$w / 2.0);
          break;
        case 'SQUARE':
          this$VectorFields.drawSquare_0(ctx, closure$w, closure$h);
          break;
      }
      var lineWidth = 1.5;
      DrawUtil_getInstance().drawLine_ovbgws$(ctx, closure$line, closure$stroke, lineWidth);
    };
  }
  VectorFields.prototype.createVectorImage_0 = function (w, h, complex, line, style, isColor) {
    var stroke = isColor ? ColorUtil_getInstance().getColor_p4p8i0$(complex) + 'AA' : Colors_getInstance().black + 'AA';
    return HtmlUtil_getInstance().preRender_yb5akz$(w, h, VectorFields$createVectorImage$lambda(style, w, this, h, line, stroke));
  };
  VectorFields.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'VectorFields',
    interfaces: []
  };
  var VectorFields_instance = null;
  function VectorFields_getInstance() {
    if (VectorFields_instance === null) {
      new VectorFields();
    }
    return VectorFields_instance;
  }
  function Queues() {
    Queues_instance = this;
    this.attackQueue = LinkedHashMap_init();
    this.damageQueue = LinkedHashMap_init();
    this.attackDelayTicks = Time_getInstance().secondsToTicks_za3lpa$(20);
    this.damageDelayTicks = Time_getInstance().secondsToTicks_za3lpa$(20);
  }
  Queues.prototype.registerAttack_x4gnsd$ = function (agent, xmps, delay) {
    if (delay === void 0)
      delay = 1;
    var attackFutureTick = World_getInstance().tick + Kotlin.imul(delay, this.attackDelayTicks) | 0;
    var attackMap = this.attackQueue.get_11rb$(attackFutureTick);
    if (attackMap == null) {
      attackMap = LinkedHashMap_init();
    }
    var $receiver = attackMap;
    var key = agent.pos;
    $receiver.put_xwzc9p$(key, xmps);
    var $receiver_0 = this.attackQueue;
    var value = attackMap;
    $receiver_0.put_xwzc9p$(attackFutureTick, value);
    var damageFutureTick = World_getInstance().tick + Kotlin.imul(delay, this.damageDelayTicks) | 0;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = xmps.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var list = element.dealDamage_912u9o$(agent);
      addAll(destination, list);
    }
    var damageList = destination;
    var soundLimit = 4;
    var tmp$_0;
    tmp$_0 = take(xmps, soundLimit).iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      SoundUtil_getInstance().playXmpSound_zbn281$(element_0.level, agent.pos);
    }
    this.damageQueue.put_xwzc9p$(damageFutureTick, damageList);
  };
  Queues.prototype.endTick_za3lpa$ = function (tick) {
    var $receiver = this.attackQueue;
    var destination = LinkedHashMap_init();
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.key > tick) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    var futureAttacks = destination;
    this.attackQueue.clear();
    this.attackQueue.putAll_a2k3zr$(futureAttacks);
    var $receiver_0 = this.damageQueue;
    var destination_0 = LinkedHashMap_init();
    var tmp$_0;
    tmp$_0 = $receiver_0.entries.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      if (element_0.key > tick) {
        destination_0.put_xwzc9p$(element_0.key, element_0.value);
      }
    }
    var futureDamages = destination_0;
    this.damageQueue.clear();
    this.damageQueue.putAll_a2k3zr$(futureDamages);
  };
  Queues.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Queues',
    interfaces: []
  };
  var Queues_instance = null;
  function Queues_getInstance() {
    if (Queues_instance === null) {
      new Queues();
    }
    return Queues_instance;
  }
  function ColorUtil() {
    ColorUtil_instance = this;
    this.MAX_RGB_0 = 255;
  }
  ColorUtil.prototype.getColor_p4p8i0$ = function (c) {
    return this.getColorFromMagnitudeAndPhase_0(c.magnitude, c.phase);
  };
  ColorUtil.prototype.getColorFromMagnitudeAndPhase_0 = function (magnitude, phase) {
    if (magnitude <= 1.0 / 255) {
      return Colors_getInstance().black;
    }
    var mag = Math_0.min(1.0, magnitude);
    var clippedPhase = phase < 0.0 ? phase + Constants_getInstance().tau : phase;
    var p = clippedPhase * 6.0 / Constants_getInstance().tau;
    var b = Math_0.max(0.0, p);
    var range = numberToInt(Math_0.min(5.0, b));
    var fraction = p - range;
    var rgbValues = this.getFullSpectrum_0(range, fraction);
    var maxMag = mag * 255;
    var red = numberToInt(rgbValues.first * maxMag);
    var green = numberToInt(rgbValues.second * maxMag);
    var blue = numberToInt(rgbValues.third * maxMag);
    return '#' + this.toHexString_0(red) + this.toHexString_0(green) + this.toHexString_0(blue);
  };
  ColorUtil.prototype.toHexString_0 = function (int) {
    var v = toByte(toByte(int) & kotlin_js_internal_ByteCompanionObject.MAX_VALUE);
    return String.fromCharCode(Constants_getInstance().hexChars.charCodeAt(v >>> 4)) + String.fromCharCode(toBoxedChar(Constants_getInstance().hexChars.charCodeAt(v & 15)));
  };
  ColorUtil.prototype.getFullSpectrum_0 = function (range, fraction) {
    switch (range) {
      case 0:
        return new Triple(1.0, fraction, 0.0);
      case 1:
        return new Triple(1.0 - fraction, 1.0, 0.0);
      case 2:
        return new Triple(0.0, 1.0, fraction);
      case 3:
        return new Triple(0.0, 1.0 - fraction, 1.0);
      case 4:
        return new Triple(fraction, 0.0, 1.0);
      case 5:
        return new Triple(1.0, 0.0, 1.0 - fraction);
      default:throw IllegalArgumentException_init('Out of range: ' + range);
    }
  };
  ColorUtil.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'ColorUtil',
    interfaces: []
  };
  var ColorUtil_instance = null;
  function ColorUtil_getInstance() {
    if (ColorUtil_instance === null) {
      new ColorUtil();
    }
    return ColorUtil_instance;
  }
  function Cell(position, isPassable, movementPenalty) {
    this.position = position;
    this.isPassable = isPassable;
    this.movementPenalty = movementPenalty;
  }
  Cell.prototype.getColor = function () {
    return this.isPassableInAllDirections() ? '#ffffff33' : this.isPassable ? '#00000011' : '#00000033';
  };
  function Cell$isPassableInAllDirections$isLeftPassable(this$Cell) {
    return function () {
      var tmp$, tmp$_0;
      return (tmp$_0 = (tmp$ = World_getInstance().grid.get_11rb$(new Coords(this$Cell.position.x - 1, this$Cell.position.y))) != null ? tmp$.isPassable : null) != null ? tmp$_0 : false;
    };
  }
  function Cell$isPassableInAllDirections$isRightPassable(this$Cell) {
    return function () {
      var tmp$, tmp$_0;
      return (tmp$_0 = (tmp$ = World_getInstance().grid.get_11rb$(new Coords(this$Cell.position.x + 1, this$Cell.position.y))) != null ? tmp$.isPassable : null) != null ? tmp$_0 : false;
    };
  }
  function Cell$isPassableInAllDirections$isUpPassable(this$Cell) {
    return function () {
      var tmp$, tmp$_0;
      return (tmp$_0 = (tmp$ = World_getInstance().grid.get_11rb$(new Coords(this$Cell.position.x, this$Cell.position.y - 1))) != null ? tmp$.isPassable : null) != null ? tmp$_0 : false;
    };
  }
  function Cell$isPassableInAllDirections$isDownPassable(this$Cell) {
    return function () {
      var tmp$, tmp$_0;
      return (tmp$_0 = (tmp$ = World_getInstance().grid.get_11rb$(new Coords(this$Cell.position.x, this$Cell.position.y + 1))) != null ? tmp$.isPassable : null) != null ? tmp$_0 : false;
    };
  }
  function Cell$isPassableInAllDirections$isUpLeftPassable(this$Cell) {
    return function () {
      var tmp$, tmp$_0;
      return (tmp$_0 = (tmp$ = World_getInstance().grid.get_11rb$(new Coords(this$Cell.position.x - 1, this$Cell.position.y - 1))) != null ? tmp$.isPassable : null) != null ? tmp$_0 : false;
    };
  }
  function Cell$isPassableInAllDirections$isUpRightPassable(this$Cell) {
    return function () {
      var tmp$, tmp$_0;
      return (tmp$_0 = (tmp$ = World_getInstance().grid.get_11rb$(new Coords(this$Cell.position.x + 1, this$Cell.position.y - 1))) != null ? tmp$.isPassable : null) != null ? tmp$_0 : false;
    };
  }
  function Cell$isPassableInAllDirections$isDownLeftPassable(this$Cell) {
    return function () {
      var tmp$, tmp$_0;
      return (tmp$_0 = (tmp$ = World_getInstance().grid.get_11rb$(new Coords(this$Cell.position.x - 1, this$Cell.position.y + 1))) != null ? tmp$.isPassable : null) != null ? tmp$_0 : false;
    };
  }
  function Cell$isPassableInAllDirections$isDownRightPassable(this$Cell) {
    return function () {
      var tmp$, tmp$_0;
      return (tmp$_0 = (tmp$ = World_getInstance().grid.get_11rb$(new Coords(this$Cell.position.x + 1, this$Cell.position.y + 1))) != null ? tmp$.isPassable : null) != null ? tmp$_0 : false;
    };
  }
  Cell.prototype.isPassableInAllDirections = function () {
    var isLeftPassable = Cell$isPassableInAllDirections$isLeftPassable(this);
    var isRightPassable = Cell$isPassableInAllDirections$isRightPassable(this);
    var isUpPassable = Cell$isPassableInAllDirections$isUpPassable(this);
    var isDownPassable = Cell$isPassableInAllDirections$isDownPassable(this);
    var isUpLeftPassable = Cell$isPassableInAllDirections$isUpLeftPassable(this);
    var isUpRightPassable = Cell$isPassableInAllDirections$isUpRightPassable(this);
    var isDownLeftPassable = Cell$isPassableInAllDirections$isDownLeftPassable(this);
    var isDownRightPassable = Cell$isPassableInAllDirections$isDownRightPassable(this);
    return this.isPassable && isLeftPassable() && isRightPassable() && isUpPassable() && isDownPassable() && isUpLeftPassable() && isUpRightPassable() && isDownLeftPassable() && isDownRightPassable();
  };
  Cell.prototype.toString = function () {
    return this.position.x.toString() + ':' + this.position.y.toString();
  };
  Cell.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Cell',
    interfaces: []
  };
  Cell.prototype.component1 = function () {
    return this.position;
  };
  Cell.prototype.component2 = function () {
    return this.isPassable;
  };
  Cell.prototype.component3 = function () {
    return this.movementPenalty;
  };
  Cell.prototype.copy_w9x9j7$ = function (position, isPassable, movementPenalty) {
    return new Cell(position === void 0 ? this.position : position, isPassable === void 0 ? this.isPassable : isPassable, movementPenalty === void 0 ? this.movementPenalty : movementPenalty);
  };
  Cell.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.position) | 0;
    result = result * 31 + Kotlin.hashCode(this.isPassable) | 0;
    result = result * 31 + Kotlin.hashCode(this.movementPenalty) | 0;
    return result;
  };
  Cell.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.position, other.position) && Kotlin.equals(this.isPassable, other.isPassable) && Kotlin.equals(this.movementPenalty, other.movementPenalty)))));
  };
  function Circle(center, radius) {
    this.center = center;
    this.radius = radius;
  }
  Circle.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Circle',
    interfaces: []
  };
  Circle.prototype.component1 = function () {
    return this.center;
  };
  Circle.prototype.component2 = function () {
    return this.radius;
  };
  Circle.prototype.copy_xv7m3c$ = function (center, radius) {
    return new Circle(center === void 0 ? this.center : center, radius === void 0 ? this.radius : radius);
  };
  Circle.prototype.toString = function () {
    return 'Circle(center=' + Kotlin.toString(this.center) + (', radius=' + Kotlin.toString(this.radius)) + ')';
  };
  Circle.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.center) | 0;
    result = result * 31 + Kotlin.hashCode(this.radius) | 0;
    return result;
  };
  Circle.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.center, other.center) && Kotlin.equals(this.radius, other.radius)))));
  };
  function Complex(re, im) {
    Complex$Companion_getInstance();
    if (im === void 0)
      im = 0.0;
    this.re = re;
    this.im = im;
    var x = this.addSquares_0(this.re, this.im);
    this.magnitude = Math_0.sqrt(x);
    this.mag = this.magnitude;
    this.magnitude2 = this.addSquares_0(this.re, this.im);
    var y = this.im;
    var x_0 = this.re;
    this.phase = Math_0.atan2(y, x_0);
    this.modulus = this.magnitude;
  }
  Complex.prototype.copyWithNewMagnitude_14dthe$ = function (mag) {
    return Complex$Companion_getInstance().fromMagnitudeAndPhase_lu1900$(mag, this.phase);
  };
  Complex.prototype.negate = function () {
    return new Complex(-this.re, -this.im);
  };
  Complex.prototype.conjugate = function () {
    return new Complex(this.re, -this.im);
  };
  Complex.prototype.reverse = function () {
    return new Complex(-this.re, this.im);
  };
  Complex.prototype.not = function () {
    return this.negate();
  };
  Complex.prototype.plus_p4p8i0$ = function (c) {
    return new Complex(this.re + c.re, this.im + c.im);
  };
  Complex.prototype.plus_14dthe$ = function (d) {
    return new Complex(this.re + d, this.im);
  };
  Complex.prototype.minus_p4p8i0$ = function (c) {
    return new Complex(this.re - c.re, this.im - c.im);
  };
  Complex.prototype.minus_14dthe$ = function (d) {
    return new Complex(this.re - d, this.im);
  };
  Complex.prototype.times_p4p8i0$ = function (c) {
    return new Complex(this.re * c.re - this.im * c.im, this.re * c.im + this.im * c.re);
  };
  Complex.prototype.div_p4p8i0$ = function (c) {
    if (c.re === 0.0)
      throw IllegalArgumentException_init('Real part is 0.');
    if (c.im === 0.0)
      throw IllegalArgumentException_init('Imaginary part is 0.');
    var d = this.addSquares_0(c.re, c.im);
    return new Complex((this.re * c.re + this.im * c.im) / d, (this.im * c.re - this.re * c.im) / d);
  };
  Complex.prototype.addSquares_0 = function (re, im) {
    return re * re + im * im;
  };
  Complex.prototype.toString = function () {
    var tmp$;
    if (equals(this, Complex$Companion_getInstance().I))
      tmp$ = 'i';
    else if (equals(this, new Complex(this.re)))
      tmp$ = this.re.toString();
    else if (equals(this, new Complex(this.im)))
      tmp$ = this.im.toString() + '*i';
    else {
      var imString = this.im < 0.0 ? '-' + toString(-this.im) : '+' + this.im;
      return this.re.toString() + imString + '*i';
    }
    return tmp$;
  };
  function Complex$Companion() {
    Complex$Companion_instance = this;
    this.I = new Complex(0.0, 1.0);
    this.ZERO = new Complex(0.0, 0.0);
    this.ONE = new Complex(1.0, 0.0);
  }
  Complex$Companion.prototype.selectStronger_wqtvwk$ = function (first, second) {
    return first.mag < second.mag ? first : second;
  };
  Complex$Companion.prototype.fromImaginary_14dthe$ = function (imaginary) {
    return new Complex(0.0, imaginary);
  };
  Complex$Companion.prototype.fromImaginaryInt_za3lpa$ = function (imaginary) {
    return new Complex(0.0, imaginary);
  };
  Complex$Companion.prototype.valueOf_lu1900$ = function (magnitude, phase) {
    return this.fromMagnitudeAndPhase_lu1900$(magnitude, phase);
  };
  Complex$Companion.prototype.fromMagnitudeAndPhase_lu1900$ = function (magnitude, phase) {
    return new Complex(magnitude * Math_0.cos(phase), magnitude * Math_0.sin(phase));
  };
  Complex$Companion.prototype.random = function () {
    var mag = Util_getInstance().random();
    var phase = Constants_getInstance().tau * Util_getInstance().random();
    return Complex$Companion_getInstance().fromMagnitudeAndPhase_lu1900$(mag, phase);
  };
  Complex$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Complex$Companion_instance = null;
  function Complex$Companion_getInstance() {
    if (Complex$Companion_instance === null) {
      new Complex$Companion();
    }
    return Complex$Companion_instance;
  }
  Complex.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Complex',
    interfaces: []
  };
  function Complex_init(real, imaginary, $this) {
    if (imaginary === void 0)
      imaginary = 0;
    $this = $this || Object.create(Complex.prototype);
    Complex.call($this, real, imaginary);
    return $this;
  }
  Complex.prototype.component1 = function () {
    return this.re;
  };
  Complex.prototype.component2 = function () {
    return this.im;
  };
  Complex.prototype.copy_lu1900$ = function (re, im) {
    return new Complex(re === void 0 ? this.re : re, im === void 0 ? this.im : im);
  };
  Complex.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.re) | 0;
    result = result * 31 + Kotlin.hashCode(this.im) | 0;
    return result;
  };
  Complex.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.re, other.re) && Kotlin.equals(this.im, other.im)))));
  };
  function Coords(x, y) {
    Coords$Companion_getInstance();
    this.x = x;
    this.y = y;
  }
  Coords.prototype.isOffGrid = function () {
    return this.x < 0 || this.y < 0 || this.x >= World_getInstance().shadowW() || this.y >= World_getInstance().shadowH();
  };
  Coords.prototype.isOffScreen = function () {
    return this.x < 0 || this.y < 0 || this.x >= World_getInstance().w() || this.y >= World_getInstance().h();
  };
  Coords.prototype.xDiff_lfj9be$ = function (other) {
    return this.x - other.x;
  };
  Coords.prototype.yDiff_lfj9be$ = function (other) {
    return this.y - other.y;
  };
  Coords.prototype.distanceTo_lfj9be$ = function (other) {
    var xPow = this.xDiff_lfj9be$(other) * this.xDiff_lfj9be$(other);
    var yPow = this.yDiff_lfj9be$(other) * this.yDiff_lfj9be$(other);
    var x = xPow + yPow;
    var x_0 = Math_0.sqrt(x);
    return Math_0.abs(x_0);
  };
  Coords.prototype.toShadowPos = function () {
    return Coords_init(numberToInt(this.x / Coords$Companion_getInstance().res), numberToInt(this.y / Coords$Companion_getInstance().res));
  };
  Coords.prototype.fromShadow = function () {
    return Coords_init(numberToInt(this.x * Coords$Companion_getInstance().res), numberToInt(this.y * Coords$Companion_getInstance().res));
  };
  Coords.prototype.getSurrounding_vux9f0$ = function (w, h) {
    var $receiver = listOf([new Coords(this.x - 1.0, this.y - 1.0), new Coords(this.x, this.y - 1.0), new Coords(this.x + 1.0, this.y - 1.0), new Coords(this.x - 1.0, this.y), new Coords(this.x + 1.0, this.y), new Coords(this.x - 1.0, this.y + 1.0), new Coords(this.x, this.y + 1.0), new Coords(this.x + 1.0, this.y + 1.0)]);
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.x >= 0.0 && element.x <= w - 1.0 && element.y >= 0.0 && element.y <= h - 1.0)
        destination.add_11rb$(element);
    }
    return destination;
  };
  Coords.prototype.randomNearPoint_za3lpa$ = function (radius) {
    var r = radius * Util_getInstance().random();
    var t = Constants_getInstance().tau * Util_getInstance().random();
    return new Coords(this.x + numberToInt(r * Math_0.cos(t)), this.y + numberToInt(r * Math_0.sin(t)));
  };
  Coords.prototype.toGeo = function () {
    var latitude = Coords$Companion_getInstance().minLat_0 + this.x * Coords$Companion_getInstance().pixelPartLat_0;
    var longitude = Coords$Companion_getInstance().minLng_0 - this.y * Coords$Companion_getInstance().pixelPartLng_0;
    return new GeoCoords(longitude, latitude);
  };
  Coords.prototype.isCloseForClick_0 = function (location) {
    return (new Line(location, this)).calcLength() < Dim_getInstance().portalRadius * 2;
  };
  Coords.prototype.isClose_0 = function (location) {
    return (new Line(location, this)).calcLength() < Dim_getInstance().minDistanceBetweenPortals;
  };
  Coords.prototype.findClosePortalsForClick_0 = function () {
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (this.isCloseForClick_0(element.location))
        destination.add_11rb$(element);
    }
    return destination;
  };
  Coords.prototype.findClosePortals_0 = function () {
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (this.isClose_0(element.location))
        destination.add_11rb$(element);
    }
    return destination;
  };
  Coords.prototype.hasClosePortalForClick = function () {
    return !this.findClosePortalsForClick_0().isEmpty();
  };
  Coords.prototype.hasClosePortal = function () {
    return !this.findClosePortals_0().isEmpty();
  };
  Coords.prototype.isPassable = function () {
    return !World_getInstance().grid.isEmpty() && ensureNotNull(World_getInstance().grid.get_11rb$(this.toShadowPos())).isPassable;
  };
  Coords.prototype.findClosestPortal = function () {
    return first(this.findClosePortals_0());
  };
  Coords.prototype.isBuildable = function () {
    var tmp$, tmp$_0, tmp$_1, tmp$_2, tmp$_3, tmp$_4, tmp$_5, tmp$_6;
    var r = numberToInt(Dim_getInstance().minDistancePortalToImpassable);
    return this.isPassable() && !this.hasClosePortal() && ((tmp$_0 = (tmp$ = World_getInstance().grid.get_11rb$((new Coords(this.x - r, this.y)).toShadowPos())) != null ? tmp$.isPassable : null) != null ? tmp$_0 : false) && ((tmp$_2 = (tmp$_1 = World_getInstance().grid.get_11rb$((new Coords(this.x + r, this.y)).toShadowPos())) != null ? tmp$_1.isPassable : null) != null ? tmp$_2 : false) && ((tmp$_4 = (tmp$_3 = World_getInstance().grid.get_11rb$((new Coords(this.x, this.y - r)).toShadowPos())) != null ? tmp$_3.isPassable : null) != null ? tmp$_4 : false) && ((tmp$_6 = (tmp$_5 = World_getInstance().grid.get_11rb$((new Coords(this.x, this.y + r)).toShadowPos())) != null ? tmp$_5.isPassable : null) != null ? tmp$_6 : false);
  };
  Coords.prototype.toString = function () {
    return 'X' + this.x + ':Y' + this.y;
  };
  Coords.prototype.equals = function (other) {
    return Kotlin.isType(other, Coords) && this.x === other.x && this.y === other.y;
  };
  Coords.prototype.hashCode = function () {
    return (hashCode(this.x) * 31 | 0) + hashCode(this.y) | 0;
  };
  function Coords$Companion() {
    Coords$Companion_instance = this;
    this.defaultLat_0 = 47.4220454;
    this.defaultLng_0 = 9.3733032;
    this.latDist_0 = 0.002;
    this.lngDist_0 = this.latDist_0 * Dim_getInstance().height / Dim_getInstance().width;
    this.minLat_0 = this.defaultLat_0 - this.latDist_0;
    this.minLng_0 = this.defaultLng_0 + this.lngDist_0;
    this.pixelPartLat_0 = this.latDist_0 / Dim_getInstance().width;
    this.pixelPartLng_0 = this.lngDist_0 / Dim_getInstance().height;
    this.res = 10;
    this.xMax_0 = numberToInt(Dim_getInstance().maxDeploymentRange) * 2 | 0;
  }
  Coords$Companion.prototype.createRandomNoOffset_0 = function () {
    return Coords_init(Util_getInstance().randomInt_za3lpa$(Dim_getInstance().width), Util_getInstance().randomInt_za3lpa$(Dim_getInstance().height));
  };
  Coords$Companion.prototype.createRandom_0 = function () {
    var x = Dim_getInstance().leftOffset + Util_getInstance().randomInt_za3lpa$(numberToInt(Dim_getInstance().width - Dim_getInstance().leftOffset - Dim_getInstance().rightOffset));
    var y = Dim_getInstance().topOffset + Util_getInstance().randomInt_za3lpa$(numberToInt(Dim_getInstance().height - Dim_getInstance().topOffset - Dim_getInstance().botOffset));
    return Coords_init(numberToInt(x), numberToInt(y));
  };
  Coords$Companion.prototype.createRandomForPortal = function () {
    if (HtmlUtil_getInstance().isNotRunningInBrowser()) {
      return Coords_init(Util_getInstance().randomInt_za3lpa$(Dim_getInstance().width), Util_getInstance().randomInt_za3lpa$(Dim_getInstance().height));
    }
     else {
      var $receiver = World_getInstance().passableInActionArea();
      var destination = LinkedHashMap_init();
      var tmp$;
      tmp$ = $receiver.entries.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        if (!(element.key.fromShadow().x < Dim_getInstance().maxDeploymentRange)) {
          destination.put_xwzc9p$(element.key, element.value);
        }
      }
      var destination_0 = LinkedHashMap_init();
      var tmp$_0;
      tmp$_0 = destination.entries.iterator();
      while (tmp$_0.hasNext()) {
        var element_0 = tmp$_0.next();
        if (!(element_0.key.fromShadow().x > World_getInstance().w() - Dim_getInstance().maxDeploymentRange)) {
          destination_0.put_xwzc9p$(element_0.key, element_0.value);
        }
      }
      var destination_1 = LinkedHashMap_init();
      var tmp$_1;
      tmp$_1 = destination_0.entries.iterator();
      while (tmp$_1.hasNext()) {
        var element_1 = tmp$_1.next();
        if (!element_1.key.fromShadow().hasClosePortal()) {
          destination_1.put_xwzc9p$(element_1.key, element_1.value);
        }
      }
      var grid = destination_1;
      if (!!grid.isEmpty()) {
        var message = 'Check failed.';
        throw IllegalStateException_init(message.toString());
      }
      var randomCell = first(Util_getInstance().shuffle_bemo1h$(toList(grid)));
      var pos = randomCell.first.fromShadow();
      var offset = this.res / 2 | 0;
      return new Coords(pos.x + offset, pos.y + offset);
    }
  };
  Coords$Companion.prototype.createRandomPassable_5edep5$ = function (grid) {
    return this.createRandomPassable_0(grid, 10);
  };
  Coords$Companion.prototype.createRandomPassable_0 = function (grid, retries) {
    var tmp$;
    if (HtmlUtil_getInstance().isNotRunningInBrowser()) {
      return grid.isEmpty() ? Coords_init(0, 0) : first_0(Util_getInstance().shuffle_78lngz$(grid.keys));
    }
    if (!!grid.isEmpty()) {
      var message = 'Check failed.';
      throw IllegalStateException_init(message.toString());
    }
    var random = this.createRandomNoOffset_0();
    if (ensureNotNull(grid.get_11rb$(random.toShadowPos())).isPassable) {
      tmp$ = random;
    }
     else {
      if (retries > 0) {
        tmp$ = this.createRandomPassable_0(grid, retries - 1 | 0);
      }
       else {
        console.warn('Blocked Position: ' + random);
        tmp$ = random;
      }
    }
    return tmp$;
  };
  Coords$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Coords$Companion_instance = null;
  function Coords$Companion_getInstance() {
    if (Coords$Companion_instance === null) {
      new Coords$Companion();
    }
    return Coords$Companion_instance;
  }
  Coords.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Coords',
    interfaces: []
  };
  function Coords_init(x, y, $this) {
    $this = $this || Object.create(Coords.prototype);
    Coords.call($this, x, y);
    return $this;
  }
  Coords.prototype.component1 = function () {
    return this.x;
  };
  Coords.prototype.component2 = function () {
    return this.y;
  };
  Coords.prototype.copy_lu1900$ = function (x, y) {
    return new Coords(x === void 0 ? this.x : x, y === void 0 ? this.y : y);
  };
  function Damage(value, pos, isCritical) {
    this.value = value;
    this.pos = pos;
    this.isCritical = isCritical;
  }
  Damage.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Damage',
    interfaces: []
  };
  Damage.prototype.component1 = function () {
    return this.value;
  };
  Damage.prototype.component2 = function () {
    return this.pos;
  };
  Damage.prototype.component3 = function () {
    return this.isCritical;
  };
  Damage.prototype.copy_tlalnr$ = function (value, pos, isCritical) {
    return new Damage(value === void 0 ? this.value : value, pos === void 0 ? this.pos : pos, isCritical === void 0 ? this.isCritical : isCritical);
  };
  Damage.prototype.toString = function () {
    return 'Damage(value=' + Kotlin.toString(this.value) + (', pos=' + Kotlin.toString(this.pos)) + (', isCritical=' + Kotlin.toString(this.isCritical)) + ')';
  };
  Damage.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.value) | 0;
    result = result * 31 + Kotlin.hashCode(this.pos) | 0;
    result = result * 31 + Kotlin.hashCode(this.isCritical) | 0;
    return result;
  };
  Damage.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.value, other.value) && Kotlin.equals(this.pos, other.pos) && Kotlin.equals(this.isCritical, other.isCritical)))));
  };
  function Dim_0(width, height) {
    this.width = width;
    this.height = height;
  }
  Dim_0.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Dim',
    interfaces: []
  };
  Dim_0.prototype.component1 = function () {
    return this.width;
  };
  Dim_0.prototype.component2 = function () {
    return this.height;
  };
  Dim_0.prototype.copy_vux9f0$ = function (width, height) {
    return new Dim_0(width === void 0 ? this.width : width, height === void 0 ? this.height : height);
  };
  Dim_0.prototype.toString = function () {
    return 'Dim(width=' + Kotlin.toString(this.width) + (', height=' + Kotlin.toString(this.height)) + ')';
  };
  Dim_0.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.width) | 0;
    result = result * 31 + Kotlin.hashCode(this.height) | 0;
    return result;
  };
  Dim_0.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.width, other.width) && Kotlin.equals(this.height, other.height)))));
  };
  function GeoCircle(center, radius) {
    this.center = center;
    this.radius = radius;
  }
  GeoCircle.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'GeoCircle',
    interfaces: []
  };
  GeoCircle.prototype.component1 = function () {
    return this.center;
  };
  GeoCircle.prototype.component2 = function () {
    return this.radius;
  };
  GeoCircle.prototype.copy_as11nb$ = function (center, radius) {
    return new GeoCircle(center === void 0 ? this.center : center, radius === void 0 ? this.radius : radius);
  };
  GeoCircle.prototype.toString = function () {
    return 'GeoCircle(center=' + Kotlin.toString(this.center) + (', radius=' + Kotlin.toString(this.radius)) + ')';
  };
  GeoCircle.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.center) | 0;
    result = result * 31 + Kotlin.hashCode(this.radius) | 0;
    return result;
  };
  GeoCircle.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.center, other.center) && Kotlin.equals(this.radius, other.radius)))));
  };
  function GeoCoords(lng, lat) {
    GeoCoords$Companion_getInstance();
    this.lng = lng;
    this.lat = lat;
  }
  GeoCoords.prototype.lngDiff_qi7jkn$ = function (other) {
    return this.lng - other.lng;
  };
  GeoCoords.prototype.latDiff_qi7jkn$ = function (other) {
    return this.lat - other.lat;
  };
  GeoCoords.prototype.distanceTo_qi7jkn$ = function (other) {
    var lngPow = this.lngDiff_qi7jkn$(other) * this.lngDiff_qi7jkn$(other);
    var latPow = this.latDiff_qi7jkn$(other) * this.latDiff_qi7jkn$(other);
    var x = lngPow + latPow;
    var x_0 = Math_0.sqrt(x);
    return Math_0.abs(x_0);
  };
  GeoCoords.prototype.toJson = function () {
    return JSON.parse('[' + this.lng + ',' + this.lat + ']');
  };
  GeoCoords.prototype.toString = function () {
    return 'Geo-' + this.lng + ':' + this.lat;
  };
  function GeoCoords$Companion() {
    GeoCoords$Companion_instance = this;
  }
  GeoCoords$Companion.prototype.fromStrings_rkkr90$ = function (lngString, latString) {
    var lng = lngString != null ? toDoubleOrNull(lngString) : null;
    var lat = latString != null ? toDoubleOrNull(latString) : null;
    if (lng == null)
      return null;
    if (lat == null)
      return null;
    return new GeoCoords(lng, lat);
  };
  GeoCoords$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var GeoCoords$Companion_instance = null;
  function GeoCoords$Companion_getInstance() {
    if (GeoCoords$Companion_instance === null) {
      new GeoCoords$Companion();
    }
    return GeoCoords$Companion_instance;
  }
  GeoCoords.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'GeoCoords',
    interfaces: []
  };
  GeoCoords.prototype.component1 = function () {
    return this.lng;
  };
  GeoCoords.prototype.component2 = function () {
    return this.lat;
  };
  GeoCoords.prototype.copy_lu1900$ = function (lng, lat) {
    return new GeoCoords(lng === void 0 ? this.lng : lng, lat === void 0 ? this.lat : lat);
  };
  GeoCoords.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.lng) | 0;
    result = result * 31 + Kotlin.hashCode(this.lat) | 0;
    return result;
  };
  GeoCoords.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.lng, other.lng) && Kotlin.equals(this.lat, other.lat)))));
  };
  function GeoLine(from, to) {
    this.from = from;
    this.to = to;
  }
  GeoLine.prototype.calcXdiff = function () {
    var x = this.from.lng - this.to.lng;
    return Math_0.abs(x);
  };
  GeoLine.prototype.calcYdiff = function () {
    var x = this.from.lat - this.to.lat;
    return Math_0.abs(x);
  };
  GeoLine.prototype.calcLength = function () {
    var x = this.calcXdiff() * this.calcXdiff() + this.calcYdiff() * this.calcYdiff();
    return Math_0.sqrt(x);
  };
  GeoLine.prototype.doesIntersect_suw831$ = function (other) {
    var yFromDist = this.from.lat - other.from.lat;
    var xFromDist = this.from.lng - other.from.lng;
    var xDist = this.to.lng - this.from.lng;
    var yDist = this.to.lat - this.from.lat;
    var otherXDist = other.to.lng - other.from.lng;
    var otherYDist = other.to.lat - other.from.lat;
    var denominator = otherYDist * xDist - otherXDist * yDist;
    if (numberToInt(denominator) === 0) {
      return false;
    }
    var thisResult = (xDist * yFromDist - yDist * xFromDist) / denominator;
    var otherResult = (otherXDist * yFromDist - otherYDist * xFromDist) / denominator;
    var isOnThis = otherResult > 0 && otherResult < 1;
    var isOnOther = thisResult > 0 && thisResult < 1;
    return isOnThis && isOnOther;
  };
  GeoLine.prototype.findClosestPointTo_qi7jkn$ = function (geoPoint) {
    var tmp$;
    var xDiff = this.to.lng - this.from.lng;
    var yDiff = this.to.lat - this.from.lat;
    if (!(xDiff !== 0.0 || yDiff !== 0.0)) {
      var message = 'Check failed.';
      throw IllegalStateException_init(message.toString());
    }
    var u = ((geoPoint.lng - this.from.lng) * xDiff + (geoPoint.lat - this.from.lat) * yDiff) / (xDiff * xDiff + yDiff * yDiff);
    if (u < 0)
      tmp$ = new GeoCoords(this.from.lng, this.from.lat);
    else if (u > 1)
      tmp$ = new GeoCoords(this.to.lng, this.to.lat);
    else
      tmp$ = new GeoCoords(round(this.from.lng + u * xDiff), round(this.from.lat + u * yDiff));
    return tmp$;
  };
  GeoLine.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'GeoLine',
    interfaces: []
  };
  GeoLine.prototype.component1 = function () {
    return this.from;
  };
  GeoLine.prototype.component2 = function () {
    return this.to;
  };
  GeoLine.prototype.copy_motmke$ = function (from, to) {
    return new GeoLine(from === void 0 ? this.from : from, to === void 0 ? this.to : to);
  };
  GeoLine.prototype.toString = function () {
    return 'GeoLine(from=' + Kotlin.toString(this.from) + (', to=' + Kotlin.toString(this.to)) + ')';
  };
  GeoLine.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.from) | 0;
    result = result * 31 + Kotlin.hashCode(this.to) | 0;
    return result;
  };
  GeoLine.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.from, other.from) && Kotlin.equals(this.to, other.to)))));
  };
  function AgentsTableWidget(div) {
    AgentsTableWidget$Companion_getInstance();
    this.div = div;
  }
  AgentsTableWidget.prototype.update_dr8g31$ = function (agents) {
    var tmp$;
    tmp$ = agents.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var tmp$_0;
      var row = Kotlin.isType(tmp$_0 = document.createElement('div'), HTMLDivElement) ? tmp$_0 : throwCCE();
      addClass(row, ['row']);
      var nameCol = AgentsTableWidget$Companion_getInstance().createColumn_0(element.name);
      row.append(nameCol);
      var actionCol = AgentsTableWidget$Companion_getInstance().createColumn_0(element.action.toString());
      row.append(actionCol);
      var inventoryCol = AgentsTableWidget$Companion_getInstance().createColumn_0(element.inventory.toString());
      row.append(inventoryCol);
      this.div.appendChild(row);
    }
  };
  function AgentsTableWidget$Companion() {
    AgentsTableWidget$Companion_instance = this;
  }
  AgentsTableWidget$Companion.prototype.create = function () {
    var tmp$, tmp$_0;
    var table = Kotlin.isType(tmp$ = document.createElement('div'), HTMLDivElement) ? tmp$ : throwCCE();
    var header = Kotlin.isType(tmp$_0 = document.createElement('div'), HTMLDivElement) ? tmp$_0 : throwCCE();
    addClass(header, ['row']);
    header.append(this.createColumn_0('Name'));
    header.append(this.createColumn_0('Status'));
    header.append(this.createColumn_0('Inventory'));
    table.appendChild(header);
    return new AgentsTableWidget(table);
  };
  AgentsTableWidget$Companion.prototype.createColumn_0 = function (text) {
    var tmp$;
    var column = Kotlin.isType(tmp$ = document.createElement('div'), HTMLDivElement) ? tmp$ : throwCCE();
    addClass(column, ['col-md-3', 'col-xs-3']);
    column.innerText = text;
    return column;
  };
  AgentsTableWidget$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var AgentsTableWidget$Companion_instance = null;
  function AgentsTableWidget$Companion_getInstance() {
    if (AgentsTableWidget$Companion_instance === null) {
      new AgentsTableWidget$Companion();
    }
    return AgentsTableWidget$Companion_instance;
  }
  AgentsTableWidget.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'AgentsTableWidget',
    interfaces: []
  };
  function Line(from, to) {
    Line$Companion_getInstance();
    this.from = from;
    this.to = to;
    this.fromX = this.from.x;
    this.fromY = this.from.y;
    this.toX = this.to.x;
    this.toY = this.to.y;
  }
  Line.prototype.key = function () {
    return this.from.toString() + '<--->' + this.to.toString();
  };
  Line.prototype.calcXdiff_0 = function () {
    var x = this.from.x - this.to.x;
    return Math_0.abs(x);
  };
  Line.prototype.calcYdiff_0 = function () {
    var x = this.from.y - this.to.y;
    return Math_0.abs(x);
  };
  Line.prototype.calcLength = function () {
    var x = this.calcXdiff_0() * this.calcXdiff_0() + this.calcYdiff_0() * this.calcYdiff_0();
    return Math_0.sqrt(x);
  };
  Line.prototype.calcTaxiLength = function () {
    return numberToInt(this.calcXdiff_0() + this.calcYdiff_0());
  };
  Line.prototype.center = function () {
    return new Coords((this.from.x + this.to.x) / 2, (this.from.y + this.to.y) / 2);
  };
  Line.prototype.doesIntersect_589y3w$ = function (other) {
    var yFromDist = this.from.y - other.from.y;
    var xFromDist = this.from.x - other.from.x;
    var xDist = this.to.x - this.from.x;
    var yDist = this.to.y - this.from.y;
    var otherXDist = other.to.x - other.from.x;
    var otherYDist = other.to.y - other.from.y;
    var denominator = otherYDist * xDist - otherXDist * yDist;
    if (numberToInt(denominator) === 0) {
      return false;
    }
    var thisResult = (xDist * yFromDist - yDist * xFromDist) / denominator;
    var otherResult = (otherXDist * yFromDist - otherYDist * xFromDist) / denominator;
    var isOnThis = otherResult > 0 && otherResult < 1;
    var isOnOther = thisResult > 0 && thisResult < 1;
    return isOnThis && isOnOther;
  };
  Line.prototype.findClosestPointTo_lfj9be$ = function (point) {
    var tmp$;
    var xDiff = this.to.x - this.from.x;
    var yDiff = this.to.y - this.from.y;
    if (!(xDiff !== 0.0 || yDiff !== 0.0)) {
      var message = 'Check failed.';
      throw IllegalStateException_init(message.toString());
    }
    var u = ((point.x - this.from.x) * xDiff + (point.y - this.from.y) * yDiff) / (xDiff * xDiff + yDiff * yDiff);
    if (u < 0)
      tmp$ = new Coords(this.from.x, this.from.y);
    else if (u > 1)
      tmp$ = new Coords(this.to.x, this.to.y);
    else
      tmp$ = Coords_init(numberToInt(round(this.from.x + u * xDiff)), numberToInt(round(this.from.y + u * yDiff)));
    return tmp$;
  };
  Line.prototype.isValidArea = function () {
    return this.from.x <= this.to.x && this.from.y <= this.to.y;
  };
  Line.prototype.isPointInArea_lfj9be$ = function (point) {
    return this.isValidArea() && point.x >= this.from.x && point.y >= this.from.y && point.x <= this.to.x && point.y <= this.to.y;
  };
  Line.prototype.toString = function () {
    return this.from.toString() + '-' + this.to.toString();
  };
  function Line$Companion() {
    Line$Companion_instance = this;
  }
  Line$Companion.prototype.create_tjonv8$ = function (fromX, fromY, toX, toY) {
    return new Line(Coords_init(fromX, fromY), Coords_init(toX, toY));
  };
  Line$Companion.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Companion',
    interfaces: []
  };
  var Line$Companion_instance = null;
  function Line$Companion_getInstance() {
    if (Line$Companion_instance === null) {
      new Line$Companion();
    }
    return Line$Companion_instance;
  }
  Line.$metadata$ = {
    kind: Kind_CLASS,
    simpleName: 'Line',
    interfaces: []
  };
  Line.prototype.component1 = function () {
    return this.from;
  };
  Line.prototype.component2 = function () {
    return this.to;
  };
  Line.prototype.copy_4fg3xo$ = function (from, to) {
    return new Line(from === void 0 ? this.from : from, to === void 0 ? this.to : to);
  };
  Line.prototype.hashCode = function () {
    var result = 0;
    result = result * 31 + Kotlin.hashCode(this.from) | 0;
    result = result * 31 + Kotlin.hashCode(this.to) | 0;
    return result;
  };
  Line.prototype.equals = function (other) {
    return this === other || (other !== null && (typeof other === 'object' && (Object.getPrototypeOf(this) === Object.getPrototypeOf(other) && (Kotlin.equals(this.from, other.from) && Kotlin.equals(this.to, other.to)))));
  };
  function DrawUtil() {
    DrawUtil_instance = this;
    this.CODA = 'Coda';
    this.AMARILLO = 'AmarilloUSAF';
  }
  DrawUtil.prototype.redraw = function () {
    this.clear();
    var $receiver = World_getInstance();
    XmMap_getInstance().draw();
    var tmp$;
    tmp$ = $receiver.allAgents.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      element.drawRadius_f69bme$($receiver.ctx());
    }
    var tmp$_0;
    tmp$_0 = $receiver.allPortals.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      element_0.drawResonators_f69bme$($receiver.ctx());
    }
    if (Styles_getInstance().isDrawPortalNames) {
      var tmp$_1;
      tmp$_1 = $receiver.allPortals.iterator();
      while (tmp$_1.hasNext()) {
        var element_1 = tmp$_1.next();
        element_1.drawName_f69bme$($receiver.ctx());
      }
    }
    var tmp$_2;
    tmp$_2 = $receiver.allNonFaction.iterator();
    while (tmp$_2.hasNext()) {
      var element_2 = tmp$_2.next();
      element_2.draw_f69bme$($receiver.ctx());
    }
    var tmp$_3;
    tmp$_3 = $receiver.allAgents.iterator();
    while (tmp$_3.hasNext()) {
      var element_3 = tmp$_3.next();
      element_3.draw_f69bme$($receiver.ctx());
    }
    var tmp$_4;
    tmp$_4 = $receiver.allFields().iterator();
    while (tmp$_4.hasNext()) {
      var element_4 = tmp$_4.next();
      element_4.draw_f69bme$($receiver.ctx());
    }
    var tmp$_5;
    tmp$_5 = $receiver.allLinks().iterator();
    while (tmp$_5.hasNext()) {
      var element_5 = tmp$_5.next();
      element_5.draw_f69bme$($receiver.ctx());
    }
    var tmp$_6;
    tmp$_6 = $receiver.allPortals.iterator();
    while (tmp$_6.hasNext()) {
      var element_6 = tmp$_6.next();
      element_6.drawCenter_j4cg6b$($receiver.ctx());
    }
    Attacks_getInstance().draw();
  };
  DrawUtil.prototype.clear = function () {
    this.redraw_0(World_getInstance().can, World_getInstance().ctx());
  };
  DrawUtil.prototype.clearBackground = function () {
    var maybeImage = Styles_getInstance().isDrawNoiseMap ? World_getInstance().noiseImage : null;
    this.redraw_0(World_getInstance().bgCan, World_getInstance().bgCtx(), maybeImage);
  };
  DrawUtil.prototype.clearUserInterface = function () {
    this.redraw_0(World_getInstance().uiCan, World_getInstance().uiCtx());
  };
  DrawUtil.prototype.redraw_0 = function (canvas, ctx, image) {
    if (image === void 0)
      image = null;
    canvas.width = Dim_getInstance().width;
    canvas.height = Dim_getInstance().height;
    if (image != null) {
      ctx.putImageData(image, 0.0, 0.0);
    }
     else {
      ctx.clearRect(0.0, 0.0, canvas.width, canvas.height);
    }
  };
  DrawUtil.prototype.drawNonFaction_3mzr9k$ = function (nonFaction) {
    nonFaction.draw_f69bme$(World_getInstance().ctx());
  };
  DrawUtil.prototype.drawAllNonFaction_f69bme$ = function (ctx) {
    var tmp$;
    tmp$ = World_getInstance().allNonFaction.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      element.draw_f69bme$(ctx);
    }
  };
  DrawUtil.prototype.drawAllPortals_f69bme$ = function (ctx) {
    var tmp$;
    tmp$ = World_getInstance().allPortals.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      element.drawCenter_j4cg6b$(ctx);
    }
  };
  DrawUtil.prototype.redrawUserInterface_yadwiv$ = function (firstMu, secondMu, factions) {
    this.clearUserInterface();
    MindUnits_getInstance().draw_yadwiv$(firstMu, secondMu, factions);
    CycleDisplay_getInstance().draw();
    TickDisplay_getInstance().draw();
    StatsDisplay_getInstance().draw();
    if (Styles_getInstance().isDrawCom) {
      Com_getInstance().draw_f69bme$(World_getInstance().uiCtx());
    }
    if (Styles_getInstance().isDrawTopAgents) {
      TopAgentsDisplay_getInstance().draw();
    }
    if (World_getInstance().mousePos != null) {
      this.highlightMouse_0(ensureNotNull(World_getInstance().mousePos));
    }
    if (Config_getInstance().isHighlighActionLimit) {
      ActionLimitsDisplay_getInstance().draw();
    }
     else {
      ActionLimitsDisplay_getInstance().drawTop();
    }
  };
  DrawUtil.prototype.highlightMouse_0 = function (pos) {
    var tmp$, tmp$_0, tmp$_1;
    if (World_getInstance().shadowStreetMap == null)
      return;
    else if (ActionLimitsDisplay_getInstance().isBlocked_lfj9be$(pos))
      return;
    var ctx = World_getInstance().uiCtx();
    var r = Dim_getInstance().maxDeploymentRange * Constants_getInstance().phi;
    var circle = new Circle(pos, r);
    var tempCan = Kotlin.isType(tmp$ = document.createElement('canvas'), HTMLCanvasElement) ? tmp$ : throwCCE();
    var tempCtx = Kotlin.isType(tmp$_0 = tempCan.getContext('2d'), CanvasRenderingContext2D) ? tmp$_0 : throwCCE();
    tempCan.width = 2 * numberToInt(circle.radius) | 0;
    tempCan.height = 2 * numberToInt(circle.radius) | 0;
    var xOffset = -(circle.center.x - r);
    var yOffset = -(circle.center.y - r);
    tempCtx.putImageData(ensureNotNull(World_getInstance().shadowStreetMap), xOffset, yOffset);
    ctx.beginPath();
    ctx.arc(circle.center.x, circle.center.y, circle.radius, 0.0, 2.0 * math.PI);
    ctx.clip();
    ctx.beginPath();
    ctx.drawImage(tempCan, pos.x - r, pos.y - r, 2 * r, 2 * r);
    ctx.globalAlpha = 0.5;
    if (pos.hasClosePortalForClick())
      tmp$_1 = Colors_getInstance().orange;
    else if (pos.isBuildable())
      tmp$_1 = Colors_getInstance().white;
    else
      tmp$_1 = Colors_getInstance().red;
    var color = tmp$_1;
    var image = Portal$Companion_getInstance().renderPortalCenter_wc00gi$(color, PortalLevel$ZERO_getInstance());
    ctx.drawImage(image, pos.x - (image.width / 2 | 0), pos.y - (image.height / 2 | 0));
    ctx.globalAlpha = 1.0;
  };
  function DrawUtil$renderBarImage$lambda(closure$color, closure$w, closure$h, closure$lineWidth, closure$pWidth) {
    return function (ctx) {
      if (!equals(closure$color, Colors_getInstance().white)) {
        var path = new Path2D();
        path.moveTo(0.0, 0.0);
        path.lineTo(closure$w, 0.0);
        path.lineTo(closure$w, closure$h);
        path.lineTo(0.0, closure$h);
        path.lineTo(0.0, 0.0);
        path.closePath();
        DrawUtil_getInstance().drawPath_0(ctx, path, Colors_getInstance().black, closure$lineWidth);
        var fillPath = new Path2D();
        fillPath.moveTo(0.0, 0.0);
        fillPath.lineTo(closure$pWidth, 0.0);
        fillPath.lineTo(closure$pWidth, closure$h);
        fillPath.lineTo(0.0, closure$h);
        fillPath.lineTo(0.0, 0.0);
        fillPath.closePath();
        DrawUtil_getInstance().drawPath_0(ctx, fillPath, Colors_getInstance().black, closure$lineWidth, closure$color);
      }
    };
  }
  DrawUtil.prototype.renderBarImage_ewpgoy$ = function (color, health, h, w, lineWidth) {
    var pWidth = Kotlin.imul(health, w) / 100 | 0;
    return HtmlUtil_getInstance().preRender_yb5akz$(w, h, DrawUtil$renderBarImage$lambda(color, w, h, lineWidth, pWidth));
  };
  DrawUtil.prototype.drawRect_dve0j6$ = function (ctx, pos, h, w, fillStyle, strokeStyle, lineWidth) {
    this.drawExactRect_nmgd9k$(ctx, pos.x, pos.y, h, w, fillStyle, strokeStyle, lineWidth);
  };
  DrawUtil.prototype.drawExactRect_nmgd9k$ = function (ctx, x, y, h, w, fillStyle, strokeStyle, lineWidth) {
    ctx.fillStyle = fillStyle;
    ctx.fillRect(x, y, w, -h);
    ctx.fill();
    ctx.strokeStyle = strokeStyle;
    ctx.lineWidth = lineWidth;
    ctx.beginPath();
    ctx.strokeRect(x, y, w, -h);
    ctx.closePath();
    ctx.stroke();
  };
  DrawUtil.prototype.drawGrid = function () {
    var $receiver = World_getInstance();
    if ($receiver.isReady) {
      var tmp$;
      tmp$ = $receiver.grid.entries.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        var pos = element.key.fromShadow();
        var cell = element.value;
        $receiver.bgCtx().fillStyle = cell.getColor();
        var w = Coords$Companion_getInstance().res - 1.0;
        var h = w;
        $receiver.bgCtx().fillRect(pos.x + 1, pos.y + 1, w, h);
        $receiver.bgCtx().fill();
      }
    }
  };
  DrawUtil.prototype.drawText_omkwws$ = function (ctx, coords, text, fillStyle, fontSize, fontName) {
    ctx.textAlign = 'start';
    ctx.font = fontSize.toString() + ("px '" + fontName + "'");
    ctx.fillStyle = fillStyle;
    var xOff = (fontSize / 2 | 0) - 2 | 0;
    var yOff = fontSize / 3 | 0;
    ctx.fillText(text, coords.x - xOff, coords.y + yOff);
  };
  DrawUtil.prototype.strokeText_lowmm9$ = function (ctx, pos, text, fillStyle, fontSize, fontName, lineWidth, strokeStyle, textAlign) {
    if (fontName === void 0)
      fontName = this.CODA;
    if (lineWidth === void 0)
      lineWidth = 0.0;
    if (strokeStyle === void 0)
      strokeStyle = Colors_getInstance().black;
    if (textAlign === void 0) {
      textAlign = 'start';
    }
    var xOff = fontSize / 2.0 - 2;
    var yOff = fontSize / 3.0;
    ctx.beginPath();
    ctx.font = fontSize.toString() + ("px '" + fontName + "'");
    ctx.fillStyle = fillStyle;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.textAlign = textAlign;
    if (lineWidth > 0.0) {
      ctx.lineWidth = lineWidth;
      ctx.strokeStyle = strokeStyle;
      ctx.strokeText(text, pos.x - xOff, pos.y + yOff);
    }
    ctx.fillText(text, pos.x - xOff, pos.y + yOff);
    ctx.closePath();
    if (lineWidth > 0.0) {
      ctx.stroke();
    }
  };
  DrawUtil.prototype.drawCircle_3kie0f$ = function (ctx, circle, strokeStyle, lineWidth, fillStyle, alpha) {
    if (fillStyle === void 0)
      fillStyle = null;
    if (alpha === void 0)
      alpha = 1.0;
    ctx.globalAlpha = alpha;
    ctx.strokeStyle = strokeStyle;
    ctx.lineWidth = lineWidth;
    ctx.beginPath();
    ctx.arc(circle.center.x, circle.center.y, circle.radius, 0.0, 2.0 * math.PI);
    ctx.closePath();
    ctx.stroke();
    if (fillStyle != null) {
      ctx.fillStyle = fillStyle;
      ctx.fill();
    }
    ctx.globalAlpha = 1.0;
  };
  DrawUtil.prototype.drawPath_0 = function (ctx, path, strokeStyle, lineWidth, fillStyle, alpha) {
    if (fillStyle === void 0)
      fillStyle = null;
    if (alpha === void 0)
      alpha = 1.0;
    ctx.globalAlpha = alpha;
    if (fillStyle != null) {
      ctx.fillStyle = fillStyle;
      ctx.fill(path);
    }
    ctx.strokeStyle = strokeStyle;
    ctx.lineWidth = lineWidth;
    ctx.beginPath();
    ctx.stroke(path);
    ctx.closePath();
    ctx.stroke();
    ctx.globalAlpha = 1.0;
  };
  DrawUtil.prototype.drawLine_ovbgws$ = function (ctx, line, strokeStyle, lineWidth, alpha) {
    if (alpha === void 0)
      alpha = 1.0;
    ctx.globalAlpha = alpha;
    ctx.strokeStyle = strokeStyle;
    ctx.lineWidth = lineWidth;
    ctx.beginPath();
    ctx.moveTo(line.from.x, line.from.y);
    ctx.lineTo(line.to.x, line.to.y);
    ctx.closePath();
    ctx.stroke();
    ctx.globalAlpha = 1.0;
  };
  DrawUtil.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'DrawUtil',
    interfaces: []
  };
  var DrawUtil_instance = null;
  function DrawUtil_getInstance() {
    if (DrawUtil_instance === null) {
      new DrawUtil();
    }
    return DrawUtil_instance;
  }
  function HtmlUtil() {
    HtmlUtil_instance = this;
    this.intervalID_0 = 0;
    this.PAUSE_BUTTON_ID_0 = 'pauseButton';
    this.LOCATION_DROPDOWN_ID_0 = 'locationSelect';
    this.SOUND_CHECKBOX_ID = 'soundCheckbox';
  }
  HtmlUtil.prototype.isRunningInBrowser = function () {
    return !equals(typeof document, 'undefined');
  };
  HtmlUtil.prototype.isNotRunningInBrowser = function () {
    return !this.isRunningInBrowser();
  };
  HtmlUtil.prototype.isLocal = function () {
    var tmp$, tmp$_0, tmp$_1;
    return this.isRunningInBrowser() && ((tmp$_1 = (tmp$_0 = (tmp$ = document.location) != null ? tmp$.href : null) != null ? contains(tmp$_0, 'localhost') : null) != null ? tmp$_1 : false);
  };
  HtmlUtil.prototype.isQuickstart = function () {
    var tmp$;
    return this.isRunningInBrowser() && (Kotlin.isType(tmp$ = document.getElementById('quickstart'), HTMLInputElement) ? tmp$ : throwCCE()).checked;
  };
  function HtmlUtil$tick$lambda(it) {
    var tmp$;
    DrawUtil_getInstance().redraw();
    var factions = to(ensureNotNull(World_getInstance().userFaction), ensureNotNull((tmp$ = World_getInstance().userFaction) != null ? tmp$.enemy() : null));
    var enlMu = World_getInstance().calcTotalMu_bip15f$(Faction$ENL_getInstance());
    var resMu = World_getInstance().calcTotalMu_bip15f$(Faction$RES_getInstance());
    Cycle$Companion_getInstance().updateCheckpoints_qt1dr2$(World_getInstance().tick, enlMu, resMu);
    var firstMu = factions.first === Faction$ENL_getInstance() ? enlMu : resMu;
    var secondMu = factions.first === Faction$RES_getInstance() ? enlMu : resMu;
    DrawUtil_getInstance().redrawUserInterface_yadwiv$(firstMu, secondMu, factions);
    var tmp$_0;
    tmp$_0 = World_getInstance();
    tmp$_0.tick = tmp$_0.tick + 1 | 0;
    return Unit;
  }
  HtmlUtil.prototype.tick_0 = function () {
    if (!World_getInstance().isReady) {
      return;
    }
    var $receiver = World_getInstance().allAgents;
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(item.act());
    }
    var nextAgents = toSet(destination);
    XmMap_getInstance().updateStrayXm();
    World_getInstance().allAgents.clear();
    World_getInstance().allAgents.addAll_brywnq$(nextAgents);
    var tmp$_0;
    tmp$_0 = World_getInstance().allNonFaction.iterator();
    while (tmp$_0.hasNext()) {
      var element = tmp$_0.next();
      element.act();
    }
    window.requestAnimationFrame(HtmlUtil$tick$lambda);
  };
  function HtmlUtil$load$lambda(this$HtmlUtil) {
    return function (event) {
      this$HtmlUtil.handleMouseClick_0(event);
      return Unit;
    };
  }
  function HtmlUtil$load$lambda$lambda(this$HtmlUtil) {
    return function () {
      this$HtmlUtil.tick_0();
      return Unit;
    };
  }
  function HtmlUtil$load$lambda_0(this$HtmlUtil) {
    return function (it) {
      this$HtmlUtil.intervalID_0 = this$HtmlUtil.pauseHandler_0(this$HtmlUtil.intervalID_0, HtmlUtil$load$lambda$lambda(this$HtmlUtil));
      return Unit;
    };
  }
  function HtmlUtil$load$lambda_1(this$HtmlUtil) {
    return function (it) {
      this$HtmlUtil.mapChangeHandler_0();
      return Unit;
    };
  }
  function HtmlUtil$load$lambda_2(this$HtmlUtil) {
    return function (event) {
      this$HtmlUtil.handleMouseMove_0(event);
      return Unit;
    };
  }
  function HtmlUtil$load$lambda_3(this$HtmlUtil) {
    return function (event) {
      this$HtmlUtil.handleMouseMove_0(event);
      return Unit;
    };
  }
  HtmlUtil.prototype.load = function () {
    var tmp$, tmp$_0, tmp$_1;
    if (this.isNotRunningInBrowser())
      return;
    var rootDiv = Kotlin.isType(tmp$ = document.getElementById('root'), HTMLDivElement) ? tmp$ : throwCCE();
    addClass(rootDiv, ['container']);
    World_getInstance().can = this.createCanvas_0('mainCanvas');
    World_getInstance().bgCan = this.createCanvas_0('backgroundCanvas');
    World_getInstance().uiCan = this.createCanvas_0('uiCanvas');
    World_getInstance().uiCan.addEventListener('click', HtmlUtil$load$lambda(this), false);
    rootDiv.append(this.createCanvasDiv_0());
    var controlDiv = this.createControlDiv_0();
    var buttonDiv = Kotlin.isType(tmp$_0 = document.createElement('div'), HTMLDivElement) ? tmp$_0 : throwCCE();
    addClass(buttonDiv, ['buttonDiv']);
    var pauseButton = this.createButton_0(this.PAUSE_BUTTON_ID_0, 'topButton', 'Stop', HtmlUtil$load$lambda_0(this));
    addClass(pauseButton, ['non', 'amarillo']);
    buttonDiv.append(pauseButton);
    var dropDown = this.createDropdown_0(this.LOCATION_DROPDOWN_ID_0, HtmlUtil$load$lambda_1(this));
    var selectionName = (tmp$_1 = this.getLocationNameFromUrl_0()) != null ? tmp$_1 : 'unknown';
    this.setLocationDropdownSelection_0(dropDown, selectionName);
    buttonDiv.append(dropDown);
    buttonDiv.append(this.createSoundSpan_0());
    buttonDiv.append(this.createSatSpan_0());
    controlDiv.append(buttonDiv);
    rootDiv.append(controlDiv);
    controlDiv.addEventListener('mousemove', HtmlUtil$load$lambda_2(this), false);
    rootDiv.addEventListener('mousemove', HtmlUtil$load$lambda_3(this), false);
    var popupId = 'popup';
    rootDiv.append(this.createPopup_0(popupId));
    var maybeFaction = this.getFactionFromUrl_0();
    if (maybeFaction != null) {
      this.chooseUserFaction_0(maybeFaction);
    }
     else {
      if (this.isLocal()) {
        this.chooseUserFaction_0(Faction$Companion_getInstance().random());
      }
    }
    this.initWorld_0();
  };
  function HtmlUtil$createPopup$createButton$lambda(closure$faction, this$HtmlUtil) {
    return function (it) {
      this$HtmlUtil.chooseUserFaction_0(closure$faction);
      return Unit;
    };
  }
  function HtmlUtil$createPopup$createButton(this$HtmlUtil) {
    return function (faction) {
      var tmp$;
      var button = Kotlin.isType(tmp$ = document.createElement('button'), HTMLButtonElement) ? tmp$ : throwCCE();
      button.id = faction.abbr.toLowerCase() + 'Button';
      addClass(button, [faction.abbr.toLowerCase(), 'popupButton', 'amarillo']);
      button.innerText = faction.abbr.toUpperCase();
      button.onclick = HtmlUtil$createPopup$createButton$lambda(faction, this$HtmlUtil);
      return button;
    };
  }
  function HtmlUtil$createPopup$lambda(closure$quickstartCheck) {
    return function (it) {
      closure$quickstartCheck.click();
      return Unit;
    };
  }
  HtmlUtil.prototype.createPopup_0 = function (id) {
    var tmp$, tmp$_0, tmp$_1, tmp$_2, tmp$_3;
    var createButton = HtmlUtil$createPopup$createButton(this);
    var popupDiv = Kotlin.isType(tmp$ = document.createElement('div'), HTMLDivElement) ? tmp$ : throwCCE();
    popupDiv.id = id;
    addClass(popupDiv, ['popup']);
    var popupButtonDiv = Kotlin.isType(tmp$_0 = document.createElement('div'), HTMLDivElement) ? tmp$_0 : throwCCE();
    var enlButton = createButton(Faction$ENL_getInstance());
    var resButton = createButton(Faction$RES_getInstance());
    var quickstartDiv = Kotlin.isType(tmp$_1 = document.createElement('div'), HTMLDivElement) ? tmp$_1 : throwCCE();
    addClass(quickstartDiv, ['quickstartDiv']);
    var quickstartCheck = Kotlin.isType(tmp$_2 = document.createElement('input'), HTMLInputElement) ? tmp$_2 : throwCCE();
    quickstartCheck.id = 'quickstart';
    quickstartCheck.type = 'checkbox';
    quickstartCheck.checked = this.isQuickstartFromUrl_0();
    addClass(quickstartCheck, ['checkbox']);
    quickstartCheck.disabled = true;
    var quickstartLabel = Kotlin.isType(tmp$_3 = document.createElement('span'), HTMLSpanElement) ? tmp$_3 : throwCCE();
    addClass(quickstartLabel, ['coda', 'loadLabel']);
    quickstartLabel.id = 'quickstartLabel';
    quickstartLabel.innerHTML = 'Quick Start';
    quickstartLabel.onclick = HtmlUtil$createPopup$lambda(quickstartCheck);
    popupButtonDiv.append(enlButton);
    popupButtonDiv.append(resButton);
    quickstartDiv.append(quickstartCheck);
    quickstartDiv.append(quickstartLabel);
    popupDiv.append(popupButtonDiv);
    popupDiv.append(quickstartDiv);
    return popupDiv;
  };
  function HtmlUtil$createSoundSpan$lambda(closure$checkbox) {
    return function (it) {
      closure$checkbox.click();
      return Unit;
    };
  }
  HtmlUtil.prototype.createSoundSpan_0 = function () {
    var tmp$, tmp$_0, tmp$_1;
    var span = Kotlin.isType(tmp$ = document.createElement('span'), HTMLSpanElement) ? tmp$ : throwCCE();
    var checkbox = Kotlin.isType(tmp$_0 = document.createElement('input'), HTMLInputElement) ? tmp$_0 : throwCCE();
    checkbox.id = this.SOUND_CHECKBOX_ID;
    checkbox.type = 'checkbox';
    checkbox.checked = Config_getInstance().isSoundOn;
    addClass(checkbox, ['checkbox']);
    span.append(checkbox);
    var label = Kotlin.isType(tmp$_1 = document.createElement('span'), HTMLSpanElement) ? tmp$_1 : throwCCE();
    addClass(label, ['label', 'topLabel']);
    label.id = 'soundLabel';
    label.innerHTML = 'Sound';
    label.onclick = HtmlUtil$createSoundSpan$lambda(checkbox);
    span.append(label);
    return span;
  };
  function HtmlUtil$createSatSpan$lambda(closure$checkbox) {
    return function (it) {
      if (closure$checkbox.checked)
        MapUtil_getInstance().showSatelliteMap();
      else
        MapUtil_getInstance().hideSatelliteMap();
      return Unit;
    };
  }
  function HtmlUtil$createSatSpan$lambda_0(closure$checkbox) {
    return function (it) {
      closure$checkbox.click();
      return Unit;
    };
  }
  HtmlUtil.prototype.createSatSpan_0 = function () {
    var tmp$, tmp$_0, tmp$_1;
    var span = Kotlin.isType(tmp$ = document.createElement('span'), HTMLSpanElement) ? tmp$ : throwCCE();
    var checkbox = Kotlin.isType(tmp$_0 = document.createElement('input'), HTMLInputElement) ? tmp$_0 : throwCCE();
    checkbox.id = 'satCheckbox';
    checkbox.type = 'checkbox';
    checkbox.checked = Config_getInstance().isSatOn;
    addClass(checkbox, ['checkbox']);
    checkbox.onchange = HtmlUtil$createSatSpan$lambda(checkbox);
    span.append(checkbox);
    var label = Kotlin.isType(tmp$_1 = document.createElement('span'), HTMLSpanElement) ? tmp$_1 : throwCCE();
    addClass(label, ['label', 'topLabel']);
    label.id = 'satLabel';
    label.innerHTML = 'Satellite';
    label.onclick = HtmlUtil$createSatSpan$lambda_0(checkbox);
    span.append(label);
    return span;
  };
  HtmlUtil.prototype.createCanvasDiv_0 = function () {
    var tmp$;
    var div = Kotlin.isType(tmp$ = document.createElement('div'), HTMLDivElement) ? tmp$ : throwCCE();
    div.append(World_getInstance().uiCan);
    div.append(World_getInstance().bgCan);
    div.append(World_getInstance().can);
    return div;
  };
  HtmlUtil.prototype.createControlDiv_0 = function () {
    var tmp$;
    var div = Kotlin.isType(tmp$ = document.createElement('div'), HTMLDivElement) ? tmp$ : throwCCE();
    div.id = 'top-controls';
    addClass(div, ['controls']);
    return div;
  };
  function HtmlUtil$createSliderDiv$lambda$lambda$lambda(closure$slider, this$HtmlUtil, closure$sliderValue) {
    return function (it) {
      closure$sliderValue.innerHTML = this$HtmlUtil.qDisplay_0(closure$slider.value);
      return null;
    };
  }
  HtmlUtil.prototype.createSliderDiv_0 = function (id, qValues, className, labelText, userFaction) {
    var tmp$, tmp$_0;
    var qDiv = Kotlin.isType(tmp$ = document.createElement('div'), HTMLDivElement) ? tmp$ : throwCCE();
    qDiv.id = id;
    addClass(qDiv, ['qValues', className]);
    addClass(qDiv, ['q-' + labelText.toLowerCase()]);
    var destinationsLabel = Kotlin.isType(tmp$_0 = document.createElement('div'), HTMLDivElement) ? tmp$_0 : throwCCE();
    addClass(destinationsLabel, ['label', 'qTitle']);
    destinationsLabel.innerHTML = labelText;
    qDiv.append(destinationsLabel);
    var tmp$_1;
    tmp$_1 = qValues.iterator();
    while (tmp$_1.hasNext()) {
      var element = tmp$_1.next();
      var tmp$_2, tmp$_3, tmp$_4;
      var sliderDiv = Kotlin.isType(tmp$_2 = document.createElement('div'), HTMLDivElement) ? tmp$_2 : throwCCE();
      var facts = listOf([userFaction, userFaction.enemy()]);
      var tmp$_5;
      tmp$_5 = facts.iterator();
      while (tmp$_5.hasNext()) {
        var element_0 = tmp$_5.next();
        var tmp$_6, tmp$_7;
        var slider = Kotlin.isType(tmp$_6 = document.createElement('input'), HTMLInputElement) ? tmp$_6 : throwCCE();
        slider.id = element.sliderId + element_0.nickName;
        slider.type = 'range';
        slider.min = '0.00';
        slider.max = '1.00';
        slider.step = '0.01';
        slider.value = '0.10';
        addClass(slider, ['slider', 'qSlider', element_0.abbr.toLowerCase() + 'Slider']);
        var sliderValue = Kotlin.isType(tmp$_7 = document.createElement('span'), HTMLSpanElement) ? tmp$_7 : throwCCE();
        addClass(sliderValue, ['qSliderLabel', element_0.abbr.toLowerCase() + 'Label']);
        if (element_0 !== userFaction) {
          addClass(slider, ['invisible']);
          addClass(sliderValue, ['invisible']);
        }
         else {
          slider.oninput = HtmlUtil$createSliderDiv$lambda$lambda$lambda(slider, this, sliderValue);
        }
        sliderValue.innerHTML = this.qDisplay_0(slider.value);
        sliderDiv.append(slider);
        sliderDiv.append(sliderValue);
      }
      var qSliderLabel = Kotlin.isType(tmp$_3 = document.createElement('span'), HTMLSpanElement) ? tmp$_3 : throwCCE();
      addClass(qSliderLabel, ['qSliderTextLabel']);
      if (element.icon != null) {
        var sliderImg = Kotlin.isType(tmp$_4 = document.createElement('img'), HTMLImageElement) ? tmp$_4 : throwCCE();
        sliderImg.src = element.icon.toDataURL();
        qSliderLabel.innerHTML = sliderImg.outerHTML + ' ' + element.description;
      }
       else {
        qSliderLabel.innerHTML = element.description;
      }
      sliderDiv.append(qSliderLabel);
      qDiv.append(sliderDiv);
    }
    return qDiv;
  };
  HtmlUtil.prototype.qDisplay_0 = function (qValue) {
    var tmp$;
    var fixed = padEnd(qValue, 4, 48);
    switch (fixed) {
      case '0000':
        tmp$ = '0.00';
        break;
      case '1000':
        tmp$ = '1.00';
        break;
      default:tmp$ = fixed;
        break;
    }
    return tmp$;
  };
  HtmlUtil.prototype.initWorld_0 = function () {
    var noiseAlpha = 0.8;
    var w = Dim_getInstance().width;
    var h = Dim_getInstance().height;
    SoundUtil_getInstance().playNoiseGenSound();
    World_getInstance().noiseMap = ImprovedNoise_getInstance().generateEdgeMap_224j3y$(w, h);
    World_getInstance().noiseImage = World_getInstance().createNoiseImage_bd1o91$(World_getInstance().noiseMap, w, h, noiseAlpha);
    World_getInstance().resetAllCanvas();
    ActionLimitsDisplay_getInstance().drawTop();
    var maybeCenter = this.getSelectedCenterFromUrl_0();
    var center = !equals(maybeCenter.toString(), '0,0') ? maybeCenter : Location$Companion_getInstance().random().toJSON();
    MapUtil_getInstance().loadMaps_1io40y$(center, this.onMapload_0());
  };
  HtmlUtil.prototype.closePopup_0 = function () {
    var tmp$;
    var popup = Kotlin.isType(tmp$ = document.getElementById('popup'), HTMLDivElement) ? tmp$ : throwCCE();
    addClass(popup, ['invisible']);
  };
  HtmlUtil.prototype.createQSliders_0 = function (fact) {
    var tmp$;
    var actionSliderDiv = this.createSliderDiv_0('left-sliders', QActions_getInstance().values(), 'floatLeft', 'Actions', fact);
    var destinationSliderDiv = this.createSliderDiv_0('right-sliders', QDestinations_getInstance().values(), 'floatRight', 'Destinations', fact);
    var controlDiv = Kotlin.isType(tmp$ = document.getElementById('top-controls'), HTMLDivElement) ? tmp$ : throwCCE();
    controlDiv.append(actionSliderDiv);
    controlDiv.append(destinationSliderDiv);
  };
  HtmlUtil.prototype.chooseUserFaction_0 = function (fact) {
    var tmp$;
    this.closePopup_0();
    var pauseButton = Kotlin.isType(tmp$ = document.getElementById(this.PAUSE_BUTTON_ID_0), HTMLButtonElement) ? tmp$ : throwCCE();
    addClass(pauseButton, [fact.abbr.toLowerCase()]);
    if (World_getInstance().userFaction != null) {
      console.warn('Faction ' + toString(World_getInstance().userFaction) + ' was already chosen.');
      return;
    }
    World_getInstance().userFaction = fact;
  };
  function HtmlUtil$resetInterval$lambda(this$HtmlUtil) {
    return function () {
      this$HtmlUtil.tick_0();
      return Unit;
    };
  }
  HtmlUtil.prototype.resetInterval_0 = function () {
    var tmp$, tmp$_0;
    this.intervalID_0 = (tmp$_0 = (tmp$ = document.defaultView) != null ? tmp$.setInterval(HtmlUtil$resetInterval$lambda(this), 20) : null) != null ? tmp$_0 : 0;
  };
  function HtmlUtil$pauseHandler$lambda(closure$tickFunction) {
    return function () {
      closure$tickFunction();
      return Unit;
    };
  }
  HtmlUtil.prototype.pauseHandler_0 = function (intervalID, tickFunction) {
    var tmp$, tmp$_0, tmp$_1, tmp$_2, tmp$_3;
    var pauseButton = Kotlin.isType(tmp$ = document.getElementById(this.PAUSE_BUTTON_ID_0), HTMLButtonElement) ? tmp$ : throwCCE();
    if (intervalID !== -1) {
      pauseButton.innerText = 'Start';
      (tmp$_0 = document.defaultView) != null ? (tmp$_0.clearInterval(intervalID), Unit) : null;
      tmp$_3 = -1;
    }
     else {
      pauseButton.innerText = 'Stop';
      tmp$_3 = (tmp$_2 = (tmp$_1 = document.defaultView) != null ? tmp$_1.setInterval(HtmlUtil$pauseHandler$lambda(tickFunction), 20) : null) != null ? tmp$_2 : 0;
    }
    return tmp$_3;
  };
  HtmlUtil.prototype.isBlockedByMapbox_lfj9be$ = function (pos) {
    return this.isInMapboxArea_0(pos) || this.isInOsmArea_0(pos);
  };
  HtmlUtil.prototype.isInMapboxArea_0 = function (pos) {
    var area = new Line(Coords_init(-20, Dim_getInstance().height - 40 | 0), Coords_init(90, Dim_getInstance().height));
    return pos.x > area.from.x && pos.x <= area.to.x && pos.y > area.from.y && pos.y <= area.to.y;
  };
  HtmlUtil.prototype.isInOsmArea_0 = function (pos) {
    var w = Dim_getInstance().width;
    var area = new Line(Coords_init(w - 280 | 0, Dim_getInstance().height - 30 | 0), Coords_init(w, Dim_getInstance().height));
    return pos.x > area.from.x && pos.x <= area.to.x && pos.y > area.from.y && pos.y <= area.to.y;
  };
  HtmlUtil.prototype.handleMouseClick_0 = function (event) {
    var tmp$, tmp$_0;
    if (Kotlin.isType(event, MouseEvent)) {
      var pos = this.findMousePosition_0(World_getInstance().uiCan, event);
      if (pos.hasClosePortalForClick())
        if (World_getInstance().countPortals() > 3) {
          SoundUtil_getInstance().playPortalRemovalSound_lfj9be$(pos);
          (tmp$ = document.defaultView) != null ? tmp$.setTimeout(pos.findClosestPortal().remove(), 0) : null;
        }
         else {
          SoundUtil_getInstance().playFailSound();
        }
       else if (pos.isBuildable())
        if (World_getInstance().countPortals() < 89) {
          (tmp$_0 = document.defaultView) != null ? tmp$_0.setTimeout(World_getInstance().allPortals.add_11rb$(Portal$Companion_getInstance().create_lfj9be$(pos)), 0) : null;
        }
         else {
          SoundUtil_getInstance().playFailSound();
        }
    }
     else {
      console.warn('Unhandled event: ' + event + '.');
    }
  };
  HtmlUtil.prototype.handleMouseMove_0 = function (event) {
    var tmp$;
    var pos = this.findMousePosition_0(World_getInstance().uiCan, Kotlin.isType(tmp$ = event, MouseEvent) ? tmp$ : throwCCE());
    if (ActionLimitsDisplay_getInstance().isBlocked_lfj9be$(pos)) {
      World_getInstance().mousePos = null;
      addClass(World_getInstance().uiCan, ['unclickable']);
    }
     else {
      World_getInstance().mousePos = pos;
      removeClass(World_getInstance().uiCan, ['unclickable']);
    }
  };
  HtmlUtil.prototype.findMousePosition_0 = function (canvas, mouseEvent) {
    var rect = canvas.getBoundingClientRect();
    var scaleX = canvas.width / rect.width;
    var scaleY = canvas.height / rect.height;
    var x = (mouseEvent.clientX - rect.left) * scaleX;
    var y = (mouseEvent.clientY - rect.top) * scaleY;
    return Coords_init(numberToInt(x), numberToInt(y));
  };
  HtmlUtil.prototype.maybeWidth_0 = function (id) {
    var tmp$;
    return (tmp$ = document.getElementById(id)) != null ? tmp$.clientWidth : null;
  };
  HtmlUtil.prototype.maybeHeight_0 = function (id) {
    var tmp$;
    return (tmp$ = document.getElementById(id)) != null ? tmp$.clientHeight : null;
  };
  HtmlUtil.prototype.topActionOffset = function () {
    var tmp$;
    return (tmp$ = this.maybeHeight_0('top-controls')) != null ? tmp$ : 100;
  };
  HtmlUtil.prototype.leftSliderWidth = function () {
    var tmp$;
    return (tmp$ = this.maybeWidth_0('left-sliders')) != null ? tmp$ : 241;
  };
  HtmlUtil.prototype.leftSliderHeight = function () {
    var tmp$;
    return (tmp$ = this.maybeHeight_0('left-sliders')) != null ? tmp$ : 217;
  };
  HtmlUtil.prototype.rightSliderWidth = function () {
    var tmp$;
    return (tmp$ = this.maybeWidth_0('right-sliders')) != null ? tmp$ : 213;
  };
  HtmlUtil.prototype.rightSliderHeight = function () {
    var tmp$;
    return (tmp$ = this.maybeHeight_0('right-sliders')) != null ? tmp$ : 145;
  };
  HtmlUtil.prototype.createButton_0 = function (id, className, text, callback) {
    var tmp$;
    var button = Kotlin.isType(tmp$ = document.createElement('BUTTON'), HTMLButtonElement) ? tmp$ : throwCCE();
    button.id = id;
    addClass(button, [className]);
    button.onclick = callback;
    button.innerText = text;
    return button;
  };
  HtmlUtil.prototype.createLocationOptions_0 = function () {
    var $receiver = Location$values();
    var destination = ArrayList_init($receiver.length);
    var tmp$;
    for (tmp$ = 0; tmp$ !== $receiver.length; ++tmp$) {
      var item = $receiver[tmp$];
      var tmp$_0 = destination.add_11rb$;
      var tmp$_1;
      var opt = Kotlin.isType(tmp$_1 = document.createElement('option'), HTMLOptionElement) ? tmp$_1 : throwCCE();
      opt.text = item.displayName;
      opt.value = item.toJSONString();
      tmp$_0.call(destination, opt);
    }
    return destination;
  };
  HtmlUtil.prototype.createDropdown_0 = function (id, callback) {
    var tmp$;
    var select = Kotlin.isType(tmp$ = document.createElement('select'), HTMLSelectElement) ? tmp$ : throwCCE();
    select.id = id;
    addClass(select, ['topDrop', 'amarillo']);
    select.onchange = callback;
    var tmp$_0;
    tmp$_0 = this.createLocationOptions_0().iterator();
    while (tmp$_0.hasNext()) {
      var element = tmp$_0.next();
      select.appendChild(element);
    }
    return select;
  };
  HtmlUtil.prototype.createCanvas_0 = function (className) {
    var tmp$;
    var canvas = Kotlin.isType(tmp$ = document.createElement('canvas'), HTMLCanvasElement) ? tmp$ : throwCCE();
    addClass(canvas, ['canvas', className]);
    canvas.width = Dim_getInstance().width;
    canvas.height = Dim_getInstance().height;
    return canvas;
  };
  HtmlUtil.prototype.createOffscreenCanvas_0 = function (w, h) {
    var tmp$;
    var canvas = Kotlin.isType(tmp$ = document.createElement('canvas'), HTMLCanvasElement) ? tmp$ : throwCCE();
    canvas.width = w;
    canvas.height = h;
    return canvas;
  };
  HtmlUtil.prototype.preRender_yb5akz$ = function (w, h, drawFun) {
    var offscreen = this.createOffscreenCanvas_0(w, h);
    var offscreenCtx = this.getContext2D_ap7jt0$(offscreen);
    drawFun(offscreenCtx);
    return offscreen;
  };
  HtmlUtil.prototype.getContext2D_ap7jt0$ = function (canvas) {
    var tmp$;
    return Kotlin.isType(tmp$ = canvas.getContext('2d'), CanvasRenderingContext2D) ? tmp$ : throwCCE();
  };
  function HtmlUtil$createPortals$createPortal$lambda(closure$count, closure$callback, closure$createPortal) {
    return function () {
      if (closure$count > 0) {
        var newPortal = Portal$Companion_getInstance().createRandom();
        Loading$Companion_getInstance().draw();
        LoadingText_getInstance().draw_61zpoe$('Creating Portal ' + newPortal.name);
        VectorFields_getInstance().draw_hv9zn6$(newPortal);
        World_getInstance().allPortals.add_11rb$(newPortal);
        closure$createPortal(closure$callback, closure$count - 1 | 0);
      }
       else {
        closure$callback();
      }
    };
  }
  function HtmlUtil$createPortals$createPortal(callback, count) {
    var tmp$;
    (tmp$ = document.defaultView) != null ? tmp$.setTimeout(HtmlUtil$createPortals$createPortal$lambda(count, callback, HtmlUtil$createPortals$createPortal), 0) : null;
  }
  HtmlUtil.prototype.createPortals_0 = function (callback) {
    var createPortal = HtmlUtil$createPortals$createPortal;
    LoadingText_getInstance().draw_61zpoe$('Creating Portals..');
    World_getInstance().allPortals.clear();
    createPortal(callback, 5);
  };
  HtmlUtil.prototype.createAgents_0 = function (callback) {
    World_getInstance().allAgents.clear();
    LoadingText_getInstance().draw_61zpoe$('Creating Frogs..');
    var tmp$;
    tmp$ = (new IntRange(1, Config_getInstance().startFrogs())).iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      World_getInstance().allAgents.add_11rb$(Agent$Companion_getInstance().createFrog_5edep5$(World_getInstance().grid));
    }
    LoadingText_getInstance().draw_61zpoe$('Creating Smurfs..');
    var tmp$_0;
    tmp$_0 = (new IntRange(1, Config_getInstance().startSmurfs())).iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      World_getInstance().allAgents.add_11rb$(Agent$Companion_getInstance().createSmurf_5edep5$(World_getInstance().grid));
    }
    LoadingText_getInstance().draw_61zpoe$('Creating Non-Faction..');
    World_getInstance().allNonFaction.clear();
    World_getInstance().createNonFaction_fzludj$(callback, Config_getInstance().maxFor_bip15f$(Faction$NONE_getInstance()));
  };
  function HtmlUtil$createAgentsAndPortals$lambda(closure$callback, this$HtmlUtil) {
    return function () {
      this$HtmlUtil.createAgents_0(closure$callback);
    };
  }
  HtmlUtil.prototype.createAgentsAndPortals_0 = function (callback) {
    this.createPortals_0(HtmlUtil$createAgentsAndPortals$lambda(callback, this));
  };
  HtmlUtil.prototype.isShowSatelliteMap = function () {
    var tmp$;
    return (Kotlin.isType(tmp$ = document.getElementById('satCheckbox'), HTMLInputElement) ? tmp$ : throwCCE()).checked;
  };
  function HtmlUtil$onMapload$lambda$lambda(this$HtmlUtil) {
    return function () {
      LoadingText_getInstance().draw_61zpoe$('Ready.');
      DrawUtil_getInstance().clearBackground();
      if (World_getInstance().userFaction == null) {
        this$HtmlUtil.chooseUserFaction_0(Faction$Companion_getInstance().random());
      }
      this$HtmlUtil.createQSliders_0(ensureNotNull(World_getInstance().userFaction));
      this$HtmlUtil.resetInterval_0();
      World_getInstance().isReady = true;
      if (this$HtmlUtil.isShowSatelliteMap()) {
        MapUtil_getInstance().showSatelliteMap();
      }
      return Unit;
    };
  }
  function HtmlUtil$onMapload$lambda(this$HtmlUtil) {
    return function (grid) {
      World_getInstance().grid = grid;
      if (World_getInstance().grid.isEmpty()) {
        console.error('Grid is empty!');
      }
      DrawUtil_getInstance().drawGrid();
      this$HtmlUtil.createAgentsAndPortals_0(HtmlUtil$onMapload$lambda$lambda(this$HtmlUtil));
    };
  }
  HtmlUtil.prototype.onMapload_0 = function () {
    return HtmlUtil$onMapload$lambda(this);
  };
  HtmlUtil.prototype.mapChangeHandler_0 = function () {
    var tmp$;
    var center = this.getCenterFromDropdown_0();
    var name = this.getLocationNameFromDropdown_0();
    (tmp$ = document.location) != null ? (tmp$.href = this.createNewUrl_0(center, name)) : null;
  };
  HtmlUtil.prototype.createNewUrl_0 = function (center, name) {
    if (name === void 0)
      name = 'unknown';
    var tmp$, tmp$_0, tmp$_1, tmp$_2;
    var split_0 = split(center.toString(), [',']);
    var lng = split_0.get_za3lpa$(0);
    var lat = split_0.get_za3lpa$(1);
    var url = (tmp$ = document.location) != null ? tmp$.href : null;
    var token = Constants_getInstance().token();
    var target = Constants_getInstance().targetUrl() + token;
    if ((url != null ? contains(url, token) : null) === true) {
      tmp$_0 = split(url, [token]).get_za3lpa$(0) + token;
    }
     else {
      tmp$_0 = target;
    }
    var newUrl = tmp$_0;
    var fact = (tmp$_2 = (tmp$_1 = World_getInstance().userFaction) != null ? tmp$_1.abbr : null) != null ? tmp$_2 : '';
    return this.addParameters_0(newUrl, fact, lng, lat, name, this.isQuickstart());
  };
  HtmlUtil.prototype.getCenterFromDropdown_0 = function () {
    var tmp$, tmp$_0;
    var dropdown = Kotlin.isType(tmp$ = document.getElementById(this.LOCATION_DROPDOWN_ID_0), HTMLSelectElement) ? tmp$ : throwCCE();
    var selection = Kotlin.isType(tmp$_0 = dropdown[dropdown.selectedIndex], HTMLOptionElement) ? tmp$_0 : throwCCE();
    return JSON.parse(selection.value);
  };
  HtmlUtil.prototype.getLocationNameFromDropdown_0 = function () {
    var tmp$, tmp$_0;
    var dropdown = Kotlin.isType(tmp$ = document.getElementById(this.LOCATION_DROPDOWN_ID_0), HTMLSelectElement) ? tmp$ : throwCCE();
    var selection = Kotlin.isType(tmp$_0 = dropdown[dropdown.selectedIndex], HTMLOptionElement) ? tmp$_0 : throwCCE();
    return selection.text;
  };
  HtmlUtil.prototype.setLocationDropdownSelection_0 = function (dropdown, name) {
    var tmp$;
    var cleanName = replace(name, '%20', ' ');
    var hasMatch = {v: false};
    var tmp$_0;
    tmp$_0 = until(0, dropdown.options.length).iterator();
    while (tmp$_0.hasNext()) {
      var element = tmp$_0.next();
      var tmp$_1;
      var option = Kotlin.isType(tmp$_1 = dropdown.options[element], HTMLOptionElement) ? tmp$_1 : throwCCE();
      if (equals(option.label, cleanName)) {
        dropdown.selectedIndex = element;
        hasMatch.v = true;
      }
    }
    if (!hasMatch.v) {
      var opt = Kotlin.isType(tmp$ = document.createElement('option'), HTMLOptionElement) ? tmp$ : throwCCE();
      opt.text = 'Unknown Location';
      opt.value = '[0.0,0.0]';
      dropdown.add(opt);
      dropdown.selectedIndex = dropdown.length - 1 | 0;
    }
  };
  HtmlUtil.prototype.getSelectedCenterFromUrl_0 = function () {
    var tmp$;
    var geo = this.getLngLatFromUrl_0();
    return (tmp$ = geo != null ? geo.toJson() : null) != null ? tmp$ : this.getCenterFromDropdown_0();
  };
  HtmlUtil.prototype.url_0 = function () {
    var tmp$, tmp$_0;
    return new URL((tmp$_0 = (tmp$ = document.location) != null ? tmp$.href : null) != null ? tmp$_0 : '');
  };
  HtmlUtil.prototype.getLocationNameFromUrl_0 = function () {
    return this.url_0().searchParams.get('name');
  };
  HtmlUtil.prototype.getLngLatFromUrl_0 = function () {
    var url = this.url_0();
    var lngString = url.searchParams.get('lng');
    var latString = url.searchParams.get('lat');
    return GeoCoords$Companion_getInstance().fromStrings_rkkr90$(lngString, latString);
  };
  HtmlUtil.prototype.getFactionFromUrl_0 = function () {
    return Faction$Companion_getInstance().fromString_pdl1vj$(this.url_0().searchParams.get('faction'));
  };
  HtmlUtil.prototype.isQuickstartFromUrl_0 = function () {
    var tmp$, tmp$_0;
    return (tmp$_0 = (tmp$ = this.url_0().searchParams.get('quickstart')) != null ? toBoolean(tmp$) : null) != null ? tmp$_0 : false;
  };
  HtmlUtil.prototype.addParameters_0 = function (url, faction, lng, lat, name, isQs) {
    return url + '?faction=' + faction + '&lng=' + lng + '&lat=' + lat + '&name=' + name + '&quickstart=' + isQs;
  };
  HtmlUtil.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'HtmlUtil',
    interfaces: []
  };
  var HtmlUtil_instance = null;
  function HtmlUtil_getInstance() {
    if (HtmlUtil_instance === null) {
      new HtmlUtil();
    }
    return HtmlUtil_instance;
  }
  function ImprovedNoise() {
    ImprovedNoise_instance = this;
    this.p = new Int32Array(512);
    this.permutation_0 = new Int32Array([151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180]);
    var $receiver = new IntRange(0, 255);
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      var tmp$_0 = destination.add_11rb$;
      this.p[item] = this.permutation_0[item];
      this.p[256 + item | 0] = this.p[item];
      tmp$_0.call(destination, Unit);
    }
  }
  ImprovedNoise.prototype.noiseColorInt_lu1900$ = function (x, y) {
    return 255 * ((this.noise_0(x, y) + 1) / 2);
  };
  function ImprovedNoise$noise$fade(t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }
  function ImprovedNoise$noise$lerp(t, a, b) {
    return a + t * (b - a);
  }
  function ImprovedNoise$noise$grad(hash, x, y, z) {
    var h = hash & 15;
    var u = h < 8 ? x : y;
    var v = h < 4 ? y : h === 12 || h === 14 ? x : z;
    return ((h & 1) === 0 ? u : -u) + ((h & 2) === 0 ? v : -v);
  }
  ImprovedNoise.prototype.noise_0 = function (x, y, z) {
    if (z === void 0)
      z = 0.0;
    var fade = ImprovedNoise$noise$fade;
    var lerp = ImprovedNoise$noise$lerp;
    var grad = ImprovedNoise$noise$grad;
    var xx = x;
    var yy = y;
    var zz = z;
    var xxx = numberToInt(Math_0.floor(xx)) + 255 | 0;
    var yyy = numberToInt(Math_0.floor(yy)) + 255 | 0;
    var zzz = numberToInt(Math_0.floor(zz)) + 255 | 0;
    xx -= Math_0.floor(xx);
    yy -= Math_0.floor(yy);
    zz -= Math_0.floor(zz);
    var u = fade(xx);
    var v = fade(yy);
    var w = fade(zz);
    var a = this.p[xxx] + yyy | 0;
    var aa = this.p[a] + zzz | 0;
    var ab = this.p[a + 1 | 0] + zzz | 0;
    var b = this.p[xxx + 1 | 0] + yyy | 0;
    var ba = this.p[b] + zzz | 0;
    var bb = this.p[b + 1 | 0] + zzz | 0;
    return lerp(w, lerp(v, lerp(u, grad(this.p[aa], xx, yy, zz), grad(this.p[ba], xx - 1, yy, zz)), lerp(u, grad(this.p[ab], xx, yy - 1, zz), grad(this.p[bb], xx - 1, yy - 1, zz))), lerp(v, lerp(u, grad(this.p[aa + 1 | 0], xx, yy, zz - 1), grad(this.p[ba + 1 | 0], xx - 1, yy, zz - 1)), lerp(u, grad(this.p[ab + 1 | 0], xx, yy - 1, zz - 1), grad(this.p[bb + 1 | 0], xx - 1, yy - 1, zz - 1))));
  };
  var Array_0 = Array;
  ImprovedNoise.prototype.generateRawMap_224j3y$ = function (width, height, wavelength) {
    if (wavelength === void 0)
      wavelength = 5 + Util_getInstance().random() * 5;
    var frequency = wavelength / width;
    var array = Array_0(width);
    var tmp$;
    tmp$ = array.length - 1 | 0;
    for (var i = 0; i <= tmp$; i++) {
      array[i] = new Float64Array(height);
    }
    var noise = array;
    var z = Util_getInstance().random() * 1000;
    for (var x = 0; x < width; x++) {
      for (var y = 0; y < height; y++) {
        noise[x][y] = ImprovedNoise_getInstance().noise_0(x * frequency, y * frequency, z * frequency);
      }
    }
    return noise;
  };
  ImprovedNoise.prototype.generateEdgeMap_224j3y$ = function (width, height, wavelength) {
    if (wavelength === void 0)
      wavelength = 10.0;
    var frequency = wavelength / width;
    var array = Array_0(width);
    var tmp$;
    tmp$ = array.length - 1 | 0;
    for (var i = 0; i <= tmp$; i++) {
      array[i] = new Float64Array(height);
    }
    var noise = array;
    var z = Util_getInstance().random() * 1000;
    var steps = 5.0;
    for (var x = 0; x < width; x++) {
      for (var y = 0; y < height; y++) {
        var raw = ImprovedNoise_getInstance().noise_0(x * frequency, y * frequency, z * frequency);
        var tmp$_0 = noise[x];
        var x_0 = (raw + 0.5) * steps;
        tmp$_0[y] = Math_0.floor(x_0) / steps;
      }
    }
    return noise;
  };
  ImprovedNoise.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'ImprovedNoise',
    interfaces: []
  };
  var ImprovedNoise_instance = null;
  function ImprovedNoise_getInstance() {
    if (ImprovedNoise_instance === null) {
      new ImprovedNoise();
    }
    return ImprovedNoise_instance;
  }
  function MapUtil() {
    MapUtil_instance = this;
    this.GEO_CTRL_LITERAL_0 = "new mapboxgl.GeolocateControl({'positionOptions':{'enableHighAccuracy':true,'zoom':18},'trackUserLocation':false})";
    this.INITIAL_MAP_0 = 'initialMap';
    this.MAP_0 = 'map';
    this.SHADOW_MAP_0 = 'shadowMap';
    this.INVISIBLE_0 = 'invisible';
    this.ZOOM_0 = 18;
    this.MIN_ZOOM_0 = 18;
    this.MAX_ZOOM_0 = 18;
    this.map_0 = null;
    this.initMap_0 = null;
    this.shadowMap_0 = null;
    this.OFFSCREEN_CELL_ROWS = 10;
  }
  MapUtil.prototype.initInitialMapbox_0 = function () {
    return new mapboxgl.Map({container: 'initialMap', style: 'mapbox://styles/zirteq/cjazhkywuppf42rnx453i73z5'});
  };
  MapUtil.prototype.initMapbox_0 = function () {
    return new mapboxgl.Map({container: 'map', style: 'mapbox://styles/zirteq/cjb19u1dy02a82slyklj33o6g'});
  };
  MapUtil.prototype.initShadowMap_0 = function () {
    return new mapboxgl.Map({container: 'shadowMap', style: 'mapbox://styles/zirteq/cjaq7lw9e2y7u2rn7u6xskobn'});
  };
  function MapUtil$loadMaps$lambda(closure$callback, this$MapUtil) {
    return function (initMap) {
      this$MapUtil.loadMap_0(initMap, closure$callback);
    };
  }
  MapUtil.prototype.loadMaps_1io40y$ = function (center, callback) {
    var tmp$, tmp$_0;
    (tmp$ = document.getElementById(this.MAP_0)) != null ? addClass(tmp$, [this.INVISIBLE_0]) : null;
    (tmp$_0 = document.getElementById(this.SHADOW_MAP_0)) != null ? addClass(tmp$_0, [this.INVISIBLE_0]) : null;
    this.loadInitialMap_0(center, MapUtil$loadMaps$lambda(callback, this));
  };
  function MapUtil$loadInitialMap$addLayers(this$MapUtil) {
    return function () {
      if (Styles_getInstance().use3DBuildings) {
        ensureNotNull(this$MapUtil.initMap_0).addLayer(this$MapUtil.buildingLayerConfig_0());
      }
    };
  }
  function MapUtil$loadInitialMap$lambda(closure$addLayers, closure$callback, this$MapUtil) {
    return function () {
      closure$addLayers();
      closure$callback(ensureNotNull(this$MapUtil.initMap_0));
      return Unit;
    };
  }
  function MapUtil$loadInitialMap$lambda_0(closure$addLayers, closure$callback, this$MapUtil) {
    return function () {
      closure$addLayers();
      closure$callback(ensureNotNull(this$MapUtil.initMap_0));
      return Unit;
    };
  }
  MapUtil.prototype.loadInitialMap_0 = function (center, callback) {
    var tmp$;
    (tmp$ = document.getElementById(this.INITIAL_MAP_0)) != null ? removeClass(tmp$, [this.INVISIBLE_0]) : null;
    var addLayers = MapUtil$loadInitialMap$addLayers(this);
    if (this.initMap_0 == null) {
      this.initMap_0 = this.initInitialMapbox_0();
      ensureNotNull(this.initMap_0).on('load', MapUtil$loadInitialMap$lambda(addLayers, callback, this));
      ensureNotNull(this.initMap_0).setMinZoom(18);
      ensureNotNull(this.initMap_0).setMaxZoom(18);
      ensureNotNull(this.initMap_0).setZoom(18);
      ensureNotNull(this.initMap_0).setCenter(center);
    }
     else {
      ensureNotNull(this.initMap_0).on('moveend', MapUtil$loadInitialMap$lambda_0(addLayers, callback, this));
      var options = JSON.parse(trimMargin('{' + '"' + 'center' + '"' + ': [' + center + '], ' + '"' + 'zoom' + '"' + ': 18}'));
      ensureNotNull(this.initMap_0).jumpTo(options);
    }
  };
  function MapUtil$loadMap$lambda(closure$center, closure$callback, this$MapUtil) {
    return function () {
      this$MapUtil.loadShadowMap_0(closure$center, closure$callback);
    };
  }
  function MapUtil$loadMap$lambda_0(closure$center, closure$callback, this$MapUtil) {
    return function () {
      this$MapUtil.loadShadowMap_0(closure$center, closure$callback);
    };
  }
  MapUtil.prototype.loadMap_0 = function (initMap, callback) {
    var tmp$;
    var center = initMap.getCenter();
    (tmp$ = document.getElementById(this.MAP_0)) != null ? removeClass(tmp$, [this.INVISIBLE_0]) : null;
    if (this.map_0 == null) {
      this.map_0 = this.initMapbox_0();
      ensureNotNull(this.map_0).on('load', MapUtil$loadMap$lambda(center, callback, this));
      ensureNotNull(this.map_0).addControl(new mapboxgl.GeolocateControl({positionOptions: {enableHighAccuracy: true, zoom: 18}, trackUserLocation: false}));
      ensureNotNull(this.map_0).setMinZoom(18);
      ensureNotNull(this.map_0).setMaxZoom(18);
      ensureNotNull(this.map_0).setZoom(18);
      ensureNotNull(this.map_0).setCenter(center);
    }
     else {
      ensureNotNull(this.map_0).on('moveend', MapUtil$loadMap$lambda_0(center, callback, this));
      var lng = center['lng'];
      var lat = center['lat'];
      var options = JSON.parse(trimMargin('{' + '"' + 'center' + '"' + ': [' + toString(lng) + ',' + toString(lat) + '],' + '"' + 'zoom' + '"' + ': 18}'));
      ensureNotNull(this.map_0).jumpTo(options);
    }
  };
  function MapUtil$loadShadowMap$lambda(closure$callback, this$MapUtil) {
    return function () {
      this$MapUtil.addGrid_0(closure$callback);
    };
  }
  MapUtil.prototype.loadShadowMap_0 = function (center, callback) {
    var tmp$, tmp$_0, tmp$_1;
    (tmp$ = document.getElementById(this.SHADOW_MAP_0)) != null ? (tmp$.remove(), Unit) : null;
    var div = Kotlin.isType(tmp$_0 = document.createElement('div'), HTMLDivElement) ? tmp$_0 : throwCCE();
    div.id = this.SHADOW_MAP_0;
    addClass(div, [this.SHADOW_MAP_0, 'top']);
    (tmp$_1 = document.body) != null ? (tmp$_1.append(div), Unit) : null;
    this.shadowMap_0 = this.initShadowMap_0();
    ensureNotNull(this.shadowMap_0).on('load', MapUtil$loadShadowMap$lambda(callback, this));
    ensureNotNull(this.shadowMap_0).setMinZoom(18);
    ensureNotNull(this.shadowMap_0).setMaxZoom(18);
    ensureNotNull(this.shadowMap_0).setZoom(18);
    ensureNotNull(this.shadowMap_0).setCenter(center);
  };
  MapUtil.prototype.addGrid_0 = function (callback) {
    var tmp$, tmp$_0, tmp$_1;
    var maps = document.getElementsByClassName('mapboxgl-canvas');
    var shadowMapCan = maps[2];
    var gl = shadowMapCan.getContext('webgl');
    var width = typeof (tmp$ = gl.canvas.width) === 'number' ? tmp$ : throwCCE();
    var height = typeof (tmp$_0 = gl.canvas.height) === 'number' ? tmp$_0 : throwCCE();
    var rawBuf = new Uint8Array(Kotlin.imul(width, height) * 4 | 0);
    gl.readPixels(0, 0, width, height, gl.RGBA, gl.UNSIGNED_BYTE, rawBuf);
    var imageData = World_getInstance().createStreetImage_n3vf4$(rawBuf, width, height);
    World_getInstance().shadowStreetMap = imageData;
    var grid = this.createGrid_0(imageData, width, height);
    (tmp$_1 = document.getElementById(this.SHADOW_MAP_0)) != null ? addClass(tmp$_1, [this.INVISIBLE_0]) : null;
    callback(grid);
  };
  MapUtil.prototype.buildingLayerConfig_0 = function () {
    return JSON.parse('{\n            "id": "3d-buildings",\n            "source": "composite",\n            "source-layer": "building",\n            "filter": ["==", "extrude", "true"],\n            "type": "fill-extrusion",\n            "minzoom": 15,\n            "paint": {\n                "fill-extrusion-color": "#333333",\n                "fill-extrusion-height": ["interpolate", ["linear"], ["zoom"], 15, 0, 15.05, ["get", "height"]],\n                "fill-extrusion-base": ["interpolate", ["linear"], ["zoom"], 15, 0, 15.05, ["get", "min_height"]],\n                "fill-extrusion-opacity": 0.9\n            }\n        }');
  };
  function MapUtil$createGrid$isOffScreen(closure$w, closure$h) {
    return function (pos) {
      return pos.x < 0 || pos.y < 0 || pos.x >= closure$w || pos.y >= closure$h;
    };
  }
  function MapUtil$createGrid$nextRow(closure$isOffScreen) {
    return function (tempCtx, h, x) {
      var $receiver = until(-10, h + 10 | 0);
      var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
      var tmp$;
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var item = tmp$.next();
        var tmp$_0 = destination.add_11rb$;
        var closure$isOffScreen_0 = closure$isOffScreen;
        var transform$result;
        var pos = Coords_init(x, item);
        if (closure$isOffScreen_0(pos)) {
          var isPassable = true;
          var penalty = 80;
          transform$result = to(pos, new Cell(pos, isPassable, penalty));
        }
         else {
          var scaledPixel = tempCtx.getImageData(x, item, 1.0, 1.0).data[0];
          var passabilityOffset = 32;
          var isPassable_0 = scaledPixel > passabilityOffset;
          var penalty_0 = 35 + (((255 - scaledPixel) * 65 | 0) / 255 | 0) | 0;
          transform$result = to(pos, new Cell(pos, isPassable_0, penalty_0));
        }
        tmp$_0.call(destination, transform$result);
      }
      return destination;
    };
  }
  MapUtil.prototype.createGrid_0 = function (imageData, width, height) {
    var tmp$, tmp$_0, tmp$_1, tmp$_2;
    var w = width / Coords$Companion_getInstance().res | 0;
    var h = height / Coords$Companion_getInstance().res | 0;
    var isOffScreen = MapUtil$createGrid$isOffScreen(w, h);
    var nextRow = MapUtil$createGrid$nextRow(isOffScreen);
    var unscaledCan = Kotlin.isType(tmp$ = document.createElement('canvas'), HTMLCanvasElement) ? tmp$ : throwCCE();
    var unscaledCtx = Kotlin.isType(tmp$_0 = unscaledCan.getContext('2d'), CanvasRenderingContext2D) ? tmp$_0 : throwCCE();
    unscaledCan.width = width;
    unscaledCan.height = height;
    unscaledCtx.putImageData(imageData, 0.0, 0.0);
    var tempCan = Kotlin.isType(tmp$_1 = document.createElement('canvas'), HTMLCanvasElement) ? tmp$_1 : throwCCE();
    var tempCtx = Kotlin.isType(tmp$_2 = tempCan.getContext('2d'), CanvasRenderingContext2D) ? tmp$_2 : throwCCE();
    tempCan.width = w;
    tempCan.height = h;
    var tmp$_3;
    tmp$_3 = (new IntRange(0, 3)).iterator();
    while (tmp$_3.hasNext()) {
      var element = tmp$_3.next();
      tempCan.blur();
    }
    tempCtx.drawImage(unscaledCan, 0.0, 0.0, w, h);
    var $receiver = until(-10, w + 10 | 0);
    var destination = ArrayList_init_0();
    var tmp$_4;
    tmp$_4 = $receiver.iterator();
    while (tmp$_4.hasNext()) {
      var element_0 = tmp$_4.next();
      var list = nextRow(tempCtx, h, element_0);
      addAll(destination, list);
    }
    return toMap(destination);
  };
  MapUtil.prototype.showSatelliteMap = function () {
    var tmp$, tmp$_0;
    (tmp$ = document.getElementById(this.INITIAL_MAP_0)) != null ? addClass(tmp$, [this.INVISIBLE_0]) : null;
    (tmp$_0 = document.getElementById(this.MAP_0)) != null ? removeClass(tmp$_0, [this.INVISIBLE_0]) : null;
  };
  MapUtil.prototype.hideSatelliteMap = function () {
    var tmp$, tmp$_0;
    (tmp$ = document.getElementById(this.INITIAL_MAP_0)) != null ? removeClass(tmp$, [this.INVISIBLE_0]) : null;
    (tmp$_0 = document.getElementById(this.MAP_0)) != null ? addClass(tmp$_0, [this.INVISIBLE_0]) : null;
  };
  MapUtil.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'MapUtil',
    interfaces: []
  };
  var MapUtil_instance = null;
  function MapUtil_getInstance() {
    if (MapUtil_instance === null) {
      new MapUtil();
    }
    return MapUtil_instance;
  }
  function PathUtil() {
    PathUtil_instance = this;
    this.MIN_HEAT = 35;
    this.MAX_HEAT = 100;
  }
  PathUtil.prototype.calcPosCost_0 = function (pos, heat) {
    var tmp$, tmp$_0;
    return heat + ((tmp$_0 = (tmp$ = World_getInstance().grid.get_11rb$(pos)) != null ? tmp$.movementPenalty : null) != null ? tmp$_0 : 100) | 0;
  };
  PathUtil.prototype.posToCost_0 = function (positions, heat) {
    var destination = ArrayList_init(collectionSizeOrDefault(positions, 10));
    var tmp$;
    tmp$ = positions.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(to(item, this.calcPosCost_0(item, heat)));
    }
    return toMap(destination);
  };
  PathUtil.prototype.mergeMaps_0 = function (maps) {
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = maps.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var destination_0 = ArrayList_init(element.size);
      var tmp$_0;
      tmp$_0 = element.entries.iterator();
      while (tmp$_0.hasNext()) {
        var item = tmp$_0.next();
        destination_0.add_11rb$(to(item.key, item.value));
      }
      var list = destination_0;
      addAll(destination, list);
    }
    return toMap(destination);
  };
  PathUtil.prototype.findSuccessors_0 = function (currentMap, passable, sameHeat, heat) {
    var destination = ArrayList_init(collectionSizeOrDefault(sameHeat, 10));
    var tmp$;
    tmp$ = sameHeat.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      var tmp$_0 = destination.add_11rb$;
      var successors = this.findUnmarkedSurrounding_0(item, passable, currentMap);
      tmp$_0.call(destination, to(this.posToCost_0(successors, heat), !successors.isEmpty()));
    }
    return toMap(destination);
  };
  PathUtil.prototype.calcFront_0 = function (currentMap, passable, sameHeat, heat) {
    var result = this.findSuccessors_0(currentMap, passable, sameHeat, heat);
    var front = this.mergeMaps_0(result.keys);
    var hasMore = result.values.contains_11rb$(true);
    return to(front, hasMore);
  };
  PathUtil.prototype.createWaveFront_0 = function (currentHeatMap, passable, heat) {
    var destination = LinkedHashMap_init();
    var tmp$;
    tmp$ = currentHeatMap.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.value === heat) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    var sameHeat = destination;
    var tmp$_0 = this.calcFront_0(currentHeatMap, passable, sameHeat.keys, heat);
    var layer = tmp$_0.component1()
    , hasMaybeMore = tmp$_0.component2();
    return to(layer, hasMaybeMore);
  };
  PathUtil.prototype.generateHeatMap_lfj9be$ = function (goal) {
    var tmp$, tmp$_0;
    var passable = World_getInstance().passableCells();
    var heat = 0;
    var maxHeat = 0;
    var map = LinkedHashMap_init();
    var key = goal.toShadowPos();
    var value = heat;
    map.put_xwzc9p$(key, value);
    while (true) {
      var tmp$_1 = this.createWaveFront_0(map, passable, (tmp$ = heat, heat = tmp$ + 1 | 0, tmp$));
      var layer = tmp$_1.component1()
      , hasMaybeMore = tmp$_1.component2();
      map.putAll_a2k3zr$(layer);
      var destination = ArrayList_init(layer.size);
      var tmp$_2;
      tmp$_2 = layer.entries.iterator();
      while (tmp$_2.hasNext()) {
        var item = tmp$_2.next();
        destination.add_11rb$(item.value);
      }
      var layerMax = (tmp$_0 = max(destination)) != null ? tmp$_0 : 0;
      var a = maxHeat;
      maxHeat = Math_0.max(a, layerMax);
      var overCount = heat - maxHeat | 0;
      var hasMore = hasMaybeMore || overCount < 100;
      if (!hasMore) {
        break;
      }
    }
    return map;
  };
  PathUtil.prototype.createVec_0 = function (heatMap, maxHeat, destination, pos) {
    var tmp$, tmp$_0, tmp$_1, tmp$_2, tmp$_3;
    var left = (tmp$ = heatMap.get_11rb$(new Coords(pos.x - 1, pos.y))) != null ? tmp$ : maxHeat;
    var right = (tmp$_0 = heatMap.get_11rb$(new Coords(pos.x + 1, pos.y))) != null ? tmp$_0 : maxHeat;
    var up = (tmp$_1 = heatMap.get_11rb$(new Coords(pos.x, pos.y - 1))) != null ? tmp$_1 : maxHeat;
    var down = (tmp$_2 = heatMap.get_11rb$(new Coords(pos.x, pos.y + 1))) != null ? tmp$_2 : maxHeat;
    var lr = left - right | 0;
    var ud = up - down | 0;
    var isBlocked = lr === 0 && ud === 0;
    if (!isBlocked) {
      tmp$_3 = Complex_init(lr, ud);
    }
     else {
      var xDiff = destination.x - pos.x;
      var yDiff = destination.y - pos.y;
      tmp$_3 = new Complex(xDiff, yDiff);
    }
    return tmp$_3;
  };
  PathUtil.prototype.calculateVectorField_3e8r0f$ = function (heatMap, destination) {
    var maxHeat = ensureNotNull(max(heatMap.values));
    var $receiver = World_getInstance().grid;
    var destination_0 = ArrayList_init($receiver.size);
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      var tmp$_0 = destination_0.add_11rb$;
      var raw = this.createVec_0(heatMap, maxHeat, destination, item.key);
      var vec = raw.copyWithNewMagnitude_14dthe$(1.0);
      tmp$_0.call(destination_0, to(item.key, vec));
    }
    var fields = toMap(destination_0);
    return toMap_0(this.smooth_0(fields, 3));
  };
  PathUtil.prototype.smooth_0 = function (map, count) {
    if (count > 0) {
      return this.smooth_0(this.smoothVectorMap_0(map), count - 1 | 0);
    }
     else {
      return map;
    }
  };
  PathUtil.prototype.smoothVectorMap_0 = function (map) {
    var n = 1;
    var xRange = new IntRange(-n | 0, n);
    var yRange = new IntRange(-n | 0, n);
    var destination = ArrayList_init(map.size);
    var tmp$;
    tmp$ = map.entries.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      var tmp$_0 = destination.add_11rb$;
      var pos = item.key;
      var destination_0 = ArrayList_init_0();
      var tmp$_1;
      tmp$_1 = yRange.iterator();
      while (tmp$_1.hasNext()) {
        var element = tmp$_1.next();
        var destination_1 = ArrayList_init(collectionSizeOrDefault(xRange, 10));
        var tmp$_2;
        tmp$_2 = xRange.iterator();
        while (tmp$_2.hasNext()) {
          var item_0 = tmp$_2.next();
          var tmp$_3;
          destination_1.add_11rb$((tmp$_3 = map.get_11rb$(new Coords(pos.x + item_0, pos.y + element))) != null ? tmp$_3 : Complex$Companion_getInstance().ZERO);
        }
        var list = destination_1;
        addAll(destination_0, list);
      }
      var tmp$_4;
      var accumulator = Complex$Companion_getInstance().ZERO;
      tmp$_4 = destination_0.iterator();
      while (tmp$_4.hasNext()) {
        var element_0 = tmp$_4.next();
        accumulator = accumulator.plus_p4p8i0$(element_0);
      }
      var sum = accumulator;
      var magnitude = sum.magnitude / Kotlin.imul(count(xRange), count(yRange));
      var phase = sum.phase;
      tmp$_0.call(destination, to(item.key, Complex$Companion_getInstance().fromMagnitudeAndPhase_lu1900$(magnitude, phase)));
    }
    return toMap(destination);
  };
  PathUtil.prototype.findUnmarkedSurrounding_0 = function (node, passable, heatMap) {
    var $receiver = this.findAllSurrounding_0(node);
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (!heatMap.containsKey_11rb$(element))
        destination.add_11rb$(element);
    }
    var destination_0 = ArrayList_init_0();
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      if (passable.containsKey_11rb$(element_0))
        destination_0.add_11rb$(element_0);
    }
    return toSet(destination_0);
  };
  PathUtil.prototype.findAllSurrounding_0 = function (node) {
    return listOfNotNull([new Coords(node.x - 1, node.y - 1), new Coords(node.x - 1, node.y), new Coords(node.x - 1, node.y + 1), new Coords(node.x, node.y - 1), new Coords(node.x, node.y + 1), new Coords(node.x + 1, node.y - 1), new Coords(node.x + 1, node.y), new Coords(node.x + 1, node.y + 1)]);
  };
  PathUtil.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'PathUtil',
    interfaces: []
  };
  var PathUtil_instance = null;
  function PathUtil_getInstance() {
    if (PathUtil_instance === null) {
      new PathUtil();
    }
    return PathUtil_instance;
  }
  function SoundUtil() {
    SoundUtil_instance = this;
    this.audioCtx_0 = new AudioContext();
  }
  SoundUtil.prototype.isMuted_0 = function () {
    var tmp$;
    var soundCheckbox = Kotlin.isType(tmp$ = document.getElementById(HtmlUtil_getInstance().SOUND_CHECKBOX_ID), HTMLInputElement) ? tmp$ : throwCCE();
    return !soundCheckbox.checked;
  };
  SoundUtil.prototype.volume_0 = function () {
    return this.isMuted_0() ? 0.0 : 0.4;
  };
  SoundUtil.prototype.playNoiseGenSound = function () {
    if (!Config_getInstance().isPlayInitialSound || this.isMuted_0())
      return;
    var freq = 330;
    var osc = this.createNoiseOscillator_0(freq);
    this.playSound_0(osc, this.createNoisePan_0(), 0.15, 13.0);
  };
  SoundUtil.prototype.playOffScreenLocationCreationSound = function () {
    var center = Coords_init(Dim_getInstance().width / 2 | 0, Dim_getInstance().height / 2 | 0);
    return this.playPortalCreationSound_xv7m3c$(center, 0.5);
  };
  SoundUtil.prototype.playPortalCreationSound_xv7m3c$ = function (pos, gain) {
    if (gain === void 0)
      gain = 1.0;
    if (this.isMuted_0())
      return;
    var duration = 0.5;
    var pan = pos.x / Dim_getInstance().width;
    var oscNode = this.createLinearRampOscillator_0(OscillatorType_getInstance().SINE, 120.0, 0.0, duration);
    this.playSound_0(oscNode, this.createStaticPan_0(pan), gain, duration);
  };
  SoundUtil.prototype.playPortalRemovalSound_lfj9be$ = function (pos) {
    if (this.isMuted_0())
      return;
    var duration = 0.5;
    var pan = pos.x / Dim_getInstance().width;
    var oscNode = this.createLinearRampOscillator_0(OscillatorType_getInstance().SINE, 60.0, 120.0, duration);
    this.playSound_0(oscNode, this.createStaticPan_0(pan), 1.0, duration);
  };
  SoundUtil.prototype.playCheckpointSound_2xtf47$ = function (checkpoint) {
    if (this.isMuted_0())
      return;
    var duration = 0.05;
    var pan = 0.5;
    var oscNode = this.createLinearRampOscillator_0(OscillatorType_getInstance().SINE, 440.0, 440.0, duration);
    this.playSound_0(oscNode, this.createStaticPan_0(pan), 0.5, duration);
  };
  SoundUtil.prototype.playFailSound = function () {
    if (this.isMuted_0())
      return;
    var freq = 220.0;
    var osc = this.createStaticOscillator_0(OscillatorType_getInstance().SINE, freq);
    this.playSound_0(osc, this.createNoisePan_0(), 0.1, 0.5);
  };
  SoundUtil.prototype.playCycleSound = function () {
    if (this.isMuted_0())
      return;
    var duration = 0.01;
    var pan = 0.5;
    var oscNode = this.createLinearRampOscillator_0(OscillatorType_getInstance().SINE, 220.0, 220.0, duration);
    this.playSound_0(oscNode, this.createStaticPan_0(pan), 0.5, duration);
  };
  SoundUtil.prototype.playNpcCreationSound_3mzr9k$ = function (npc) {
    if (this.isMuted_0())
      return;
    var duration = 0.02;
    var pan = npc.pos.x / Dim_getInstance().width;
    var offset = -(npc.size.offset * 120.0);
    var start = 660.0;
    var end = 660.0 + offset;
    var oscNode = this.createLinearRampOscillator_0(OscillatorType_getInstance().SINE, start, end, duration);
    this.playSound_0(oscNode, this.createStaticPan_0(pan), 0.2, duration);
  };
  SoundUtil.prototype.playHackingSound_lfj9be$ = function (pos) {
    if (this.isMuted_0())
      return;
    var freq = 500.0;
    var osc = this.createStaticOscillator_0(OscillatorType_getInstance().SINE, freq);
    var pan = pos.x / Dim_getInstance().width;
    var gain = 0.04;
    var duration = 0.02;
    this.playSound_0(osc, this.createStaticPan_0(pan), gain, duration);
  };
  SoundUtil.prototype.playGlyphingSound_lfj9be$ = function (pos) {
    if (this.isMuted_0())
      return;
    var freq = 400.0;
    var osc = this.createStaticOscillator_0(OscillatorType_getInstance().SINE, freq);
    var pan = pos.x / Dim_getInstance().width;
    var gain = 0.04;
    var duration = 0.06;
    this.playSound_0(osc, this.createStaticPan_0(pan), gain, duration);
  };
  SoundUtil.prototype.playXmpSound_zbn281$ = function (level, pos) {
    if (this.isMuted_0())
      return;
    var freq = 160.0 - (level.level * 5 | 0);
    var osc = this.createStaticOscillator_0(OscillatorType_getInstance().SQUARE, freq);
    var pan = pos.x / Dim_getInstance().width;
    var gain = 0.04 + level.level * 0.006;
    var duration = 0.005 + 0.001 * level.level;
    this.playSound_0(osc, this.createStaticPan_0(pan), gain, duration);
  };
  SoundUtil.prototype.playDeploySound_s1df0o$ = function (pos, distanceToPortal) {
    if (this.isMuted_0())
      return;
    var ratio = distanceToPortal / Dim_getInstance().maxDeploymentRange;
    var gain = 0.1;
    var duration = 0.2;
    var minFreq = 250.0;
    var baseFreq = -250.0;
    var startFreq = minFreq + baseFreq * ratio;
    var endFreq = minFreq + baseFreq * ratio * 2;
    var pan = pos.x / Dim_getInstance().width;
    var oscNode = this.createLinearRampOscillator_0(OscillatorType_getInstance().SINE, startFreq, endFreq, duration);
    this.playSound_0(oscNode, this.createStaticPan_0(pan), gain, duration);
  };
  SoundUtil.prototype.playLinkingSound_4tp95w$ = function (link) {
    if (this.isMuted_0())
      return;
    var ratio = link.getLine().calcLength() / World_getInstance().diagonalLength();
    var gain = 0.3;
    var duration = 0.04 + 0.16 * ratio;
    var minFreq = 500.0 * ratio;
    var baseFreq = 500.0;
    var startFreq = minFreq + baseFreq * ratio;
    var endFreq = minFreq + baseFreq * ratio * 2;
    var startPan = link.getLine().from.x / Dim_getInstance().width;
    var endPan = link.getLine().to.x / Dim_getInstance().width;
    var oscNode = this.createLinearRampOscillator_0(OscillatorType_getInstance().SINE, startFreq, endFreq, duration);
    var panNode = this.createLinearRampPan_0(startPan, endPan, duration);
    this.playSound_0(oscNode, panNode, gain, duration);
  };
  SoundUtil.prototype.playFieldingSound_7ltq94$ = function (field) {
    if (this.isMuted_0())
      return;
    var areaRatio = field.calculateArea() / World_getInstance().totalArea() | 0;
    var gain = 0.4;
    var minDuration = 1.0 / Constants_getInstance().phi;
    var maxDuration = 1.0;
    var diff = maxDuration - minDuration;
    var additionalDuration = diff * areaRatio;
    var duration = minDuration + additionalDuration;
    var minFreq = 70.0;
    var baseFreq = 20.0;
    var startFreq = minFreq + baseFreq * areaRatio;
    var endFreq = startFreq * 2.0;
    var startPan = field.origin.x() / Dim_getInstance().width;
    var endPan = 0.5 * (field.primaryAnchor.x() + field.secondaryAnchor.x()) / Dim_getInstance().width;
    var oscNode = this.createExponentialRampOscillator_0(OscillatorType_getInstance().TRIANGLE, startFreq, endFreq, duration);
    var panNode = this.createLinearRampPan_0(startPan, endPan, duration);
    this.playSound_0(oscNode, panNode, gain, duration);
  };
  SoundUtil.prototype.playSound_0 = function (oscNode, panNode, gain, duration) {
    var gainNode = this.createStaticGain_0(gain);
    oscNode.connect(panNode);
    panNode.connect(gainNode);
    gainNode.connect(this.audioCtx_0.destination);
    oscNode.start();
    oscNode.stop(this.now_0() + duration);
  };
  SoundUtil.prototype.now_0 = function () {
    return this.audioCtx_0.currentTime;
  };
  SoundUtil.prototype.createStaticOscillator_0 = function (type, freq) {
    var node = this.audioCtx_0.createOscillator();
    node.type = type;
    node.frequency.setTargetAtTime(freq, this.now_0(), 0.0);
    return node;
  };
  SoundUtil.prototype.createNoiseOscillator_0 = function (maxFreq) {
    var node = this.audioCtx_0.createOscillator();
    node.type = OscillatorType_getInstance().SQUARE;
    var n = this.now_0();
    var timeConstant = 0.01;
    var max = 1000;
    var tmp$;
    tmp$ = (new IntRange(0, max)).iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var freq = Util_getInstance().randomInt_za3lpa$(maxFreq - (Kotlin.imul(maxFreq, element) / max | 0) | 0);
      var tc = timeConstant * element;
      node.frequency.setTargetAtTime(freq, n + tc, timeConstant);
    }
    return node;
  };
  SoundUtil.prototype.createLinearRampOscillator_0 = function (type, startFreq, endFreq, duration) {
    var node = this.createStaticOscillator_0(type, startFreq);
    node.frequency.linearRampToValueAtTime(endFreq, this.now_0() + duration);
    return node;
  };
  SoundUtil.prototype.createExponentialRampOscillator_0 = function (type, startFreq, endFreq, duration) {
    var node = this.createStaticOscillator_0(type, startFreq);
    node.frequency.exponentialRampToValueAtTime(endFreq, this.now_0() + duration);
    return node;
  };
  SoundUtil.prototype.createStaticPan_0 = function (pan) {
    var node = this.audioCtx_0.createStereoPanner();
    node.pan.setTargetAtTime(pan, this.now_0(), 0.0);
    return node;
  };
  SoundUtil.prototype.createNoisePan_0 = function () {
    var node = this.audioCtx_0.createStereoPanner();
    var timeConstant = 0.01;
    var max = 1000;
    var n = this.now_0();
    var tmp$;
    tmp$ = (new IntRange(0, max)).iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var pan = Util_getInstance().randomInt_za3lpa$(Dim_getInstance().width) / Dim_getInstance().width;
      var tc = timeConstant * element;
      node.pan.setTargetAtTime(pan, n + tc, timeConstant);
    }
    return node;
  };
  SoundUtil.prototype.createLinearRampPan_0 = function (startPan, endPan, duration) {
    var node = this.createStaticPan_0(startPan);
    node.pan.linearRampToValueAtTime(endPan, this.now_0() + duration);
    return node;
  };
  SoundUtil.prototype.createStaticGain_0 = function (gain) {
    var node = this.audioCtx_0.createGain();
    node.gain.setTargetAtTime(gain * this.volume_0(), this.now_0(), 0.0);
    return node;
  };
  SoundUtil.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'SoundUtil',
    interfaces: []
  };
  var SoundUtil_instance = null;
  function SoundUtil_getInstance() {
    if (SoundUtil_instance === null) {
      new SoundUtil();
    }
    return SoundUtil_instance;
  }
  function Util() {
    Util_instance = this;
  }
  function Util$findNearestPortals$lambda(it) {
    return it.first;
  }
  var compareBy$lambda_15 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_18(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_18.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_18.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Util.prototype.findNearestPortals_0 = function (coords) {
    var $receiver = World_getInstance().allPortals;
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(to(item.location.distanceTo_lfj9be$(coords), item));
    }
    return toSet(sortedWith(destination, new Comparator$ObjectLiteral_18(compareBy$lambda_15(Util$findNearestPortals$lambda))));
  };
  Util.prototype.findNearestPortal_lfj9be$ = function (coords) {
    var nearest = this.findNearestPortals_0(coords);
    return !nearest.isEmpty() ? first_0(nearest).second : null;
  };
  Util.prototype.clip_qt1dr2$ = function (value, from, to) {
    var b = Math_0.min(to, value);
    return Math_0.max(from, b);
  };
  Util.prototype.clipDouble_yvo9jy$ = function (value, from, to) {
    var b = Math_0.min(to, value);
    return Math_0.max(from, b);
  };
  Util.prototype.random = function () {
    var tmp$;
    return typeof (tmp$ = Math.random()) === 'number' ? tmp$ : throwCCE();
  };
  Util.prototype.randomBool = function () {
    return this.random() <= 0.5;
  };
  Util.prototype.randomDouble_0 = function (max) {
    return this.random() * max;
  };
  Util.prototype.randomInt_za3lpa$ = function (max) {
    return this.randomInt_vux9f0$(0, max);
  };
  Util.prototype.randomInt_vux9f0$ = function (min, max) {
    var list = toList_0(new IntRange(min, max));
    return list.get_za3lpa$(numberToInt(this.random() * list.size));
  };
  Util.prototype.shuffle_78lngz$ = function (items) {
    return toSet(this.shuffle_bemo1h$(toList_0(items)));
  };
  Util.prototype.shuffle_bemo1h$ = function (items) {
    var tmp$;
    var result = ArrayList_init_0();
    result.addAll_brywnq$(items);
    tmp$ = result.size;
    for (var i = 0; i < tmp$; i++) {
      var pos = this.randomInt_za3lpa$(result.size - 1 | 0);
      var temp = result.get_za3lpa$(i);
      result.set_wxm5ur$(i, result.get_za3lpa$(pos));
      result.set_wxm5ur$(pos, temp);
    }
    return toList_0(result);
  };
  function Util$select$lambda(it) {
    return it.first;
  }
  var compareBy$lambda_16 = wrapFunction(function () {
    var compareValues = Kotlin.kotlin.comparisons.compareValues_s00gnj$;
    return function (closure$selector) {
      return function (a, b) {
        var selector = closure$selector;
        return compareValues(selector(a), selector(b));
      };
    };
  });
  function Comparator$ObjectLiteral_19(closure$comparison) {
    this.closure$comparison = closure$comparison;
  }
  Comparator$ObjectLiteral_19.prototype.compare = function (a, b) {
    return this.closure$comparison(a, b);
  };
  Comparator$ObjectLiteral_19.$metadata$ = {kind: Kind_CLASS, interfaces: [Comparator]};
  Util.prototype.select_4u7aq8$ = function (probabilityList, default_0) {
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = probabilityList.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (!(element.first <= 0.0))
        destination.add_11rb$(element);
    }
    var list = destination;
    if (list.isEmpty()) {
      return default_0;
    }
    var tmp$_0;
    var sum = 0.0;
    tmp$_0 = list.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      sum += element_0.first;
    }
    var total = sum;
    var rand = this.randomDouble_0(total);
    var accu = {v: 0.0};
    var tmp$_1;
    tmp$_1 = sortedWith(list, new Comparator$ObjectLiteral_19(compareBy$lambda_16(Util$select$lambda))).iterator();
    while (tmp$_1.hasNext()) {
      var element_1 = tmp$_1.next();
      accu.v += element_1.first;
      if (!(element_1.first > 0.0)) {
        var message = 'Check failed.';
        throw IllegalStateException_init(message.toString());
      }
      if (accu.v >= rand) {
        return element_1.second;
      }
    }
    throw IllegalArgumentException_init('Invalid Q-values: ' + probabilityList);
  };
  Util.prototype.generatePortalName = function () {
    var separator = this.random() < 0.3 ? '-' : ' ';
    var name = this.generateName_0(3, 5);
    var values = listOf([to(1.0, ''), to(0.15, separator + 'Portal'), to(0.05, separator + 'Square'), to(0.1, separator + 'Street'), to(0.07, separator + 'Fountain'), to(0.08, separator + 'Park'), to(0.03, separator + 'Station'), to(0.02, separator + 'House'), to(0.01, separator + 'Memorial'), to(0.01, separator + 'Museum')]);
    return name + Util_getInstance().select_4u7aq8$(values, '');
  };
  Util.prototype.generateAgentName = function () {
    var name = this.generateName_0(3, 6);
    if (name.length <= 4 && this.random() < 0.5) {
      return name + toString(this.random().toString().substring(2, 4));
    }
    if (this.random() < 0.2) {
      return name + toString(this.random().toString().substring(2, 3));
    }
    return name;
  };
  Util.prototype.generateName_0 = function (minLength, maxLength) {
    var length = minLength + this.randomInt_za3lpa$(maxLength - minLength | 0) | 0;
    var firstLetter = unboxChar(this.select_4u7aq8$(this.generateFirstSelection_0(), toBoxedChar(32)));
    var $receiver = new IntRange(1, length);
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(this.select_4u7aq8$(this.generateSelection_0(), toBoxedChar(32)));
    }
    var other = joinToString(destination, '');
    var name = String.fromCharCode(firstLetter) + other;
    var temp = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    return endsWith(temp, 45) ? dropLast(temp, 1) : temp;
  };
  Util.prototype.generateSelection_0 = function () {
    return listOf([to(12.702, toBoxedChar(69)), to(9.056, toBoxedChar(84)), to(8.167, toBoxedChar(65)), to(7.507, toBoxedChar(79)), to(6.966, toBoxedChar(73)), to(6.749, toBoxedChar(78)), to(6.327, toBoxedChar(83)), to(6.094, toBoxedChar(72)), to(5.987, toBoxedChar(82)), to(4.253, toBoxedChar(68)), to(4.025, toBoxedChar(76)), to(2.782, toBoxedChar(67)), to(2.758, toBoxedChar(85)), to(2.406, toBoxedChar(77)), to(2.36, toBoxedChar(87)), to(2.228, toBoxedChar(70)), to(2.015, toBoxedChar(71)), to(1.974, toBoxedChar(89)), to(1.929, toBoxedChar(80)), to(1.492, toBoxedChar(66)), to(0.978, toBoxedChar(86)), to(0.772, toBoxedChar(75)), to(0.153, toBoxedChar(74)), to(0.15, toBoxedChar(88)), to(0.095, toBoxedChar(81)), to(0.074, toBoxedChar(90))]);
  };
  Util.prototype.generateFirstSelection_0 = function () {
    return listOf([to(15.978, toBoxedChar(84)), to(11.682, toBoxedChar(65)), to(7.631, toBoxedChar(79)), to(7.294, toBoxedChar(73)), to(6.686, toBoxedChar(83)), to(5.497, toBoxedChar(87)), to(5.238, toBoxedChar(67)), to(4.434, toBoxedChar(66)), to(4.319, toBoxedChar(80)), to(4.2, toBoxedChar(72)), to(4.027, toBoxedChar(70)), to(3.826, toBoxedChar(77)), to(3.174, toBoxedChar(68)), to(2.826, toBoxedChar(82)), to(2.799, toBoxedChar(69)), to(2.415, toBoxedChar(76)), to(2.284, toBoxedChar(78)), to(1.642, toBoxedChar(71)), to(1.183, toBoxedChar(85)), to(0.824, toBoxedChar(86)), to(0.763, toBoxedChar(89)), to(0.511, toBoxedChar(74)), to(0.456, toBoxedChar(75)), to(0.222, toBoxedChar(81)), to(0.045, toBoxedChar(88)), to(0.045, toBoxedChar(90))]);
  };
  Util.prototype.degToRad_14dthe$ = function (degrees) {
    return degrees * math.PI / 180;
  };
  Util.prototype.radToDeg_14dthe$ = function (radians) {
    return radians * 180 / math.PI;
  };
  Util.prototype.fixTime_0 = function (v) {
    return v.toString().length <= 1 ? padStart(v.toString(), 2, 48) : v.toString();
  };
  Util.prototype.formatSeconds_za3lpa$ = function (absSeconds) {
    var seconds = absSeconds % 60;
    var x = absSeconds / 60.0;
    var minutes = numberToInt(Math_0.floor(x)) % 60;
    var x_0 = absSeconds / 3600.0;
    var hours = numberToInt(Math_0.floor(x_0));
    return this.fixTime_0(hours) + ':' + this.fixTime_0(minutes) + ':' + this.fixTime_0(seconds);
  };
  Util.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'Util',
    interfaces: []
  };
  var Util_instance = null;
  function Util_getInstance() {
    if (Util_instance === null) {
      new Util();
    }
    return Util_instance;
  }
  function World() {
    World_instance = this;
    this.tick = 0;
    this.isReady = false;
    this.userFaction = null;
    this.can_v8ttwa$_0 = this.can_v8ttwa$_0;
    this.bgCan_izup8r$_0 = this.bgCan_izup8r$_0;
    this.uiCan_s0t3x6$_0 = this.uiCan_s0t3x6$_0;
    this.mousePos = null;
    this.noiseMap_ft1fdo$_0 = this.noiseMap_ft1fdo$_0;
    this.noiseImage_c4tqbn$_0 = this.noiseImage_c4tqbn$_0;
    this.shadowStreetMap = null;
    this.grid_pwdzco$_0 = this.grid_pwdzco$_0;
    this.allAgents = LinkedHashSet_init();
    var $receiver = this.allAgents;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.faction === Faction$ENL_getInstance())
        destination.add_11rb$(element);
    }
    this.frogs = toSet(destination);
    var $receiver_0 = this.allAgents;
    var destination_0 = ArrayList_init_0();
    var tmp$_0;
    tmp$_0 = $receiver_0.iterator();
    while (tmp$_0.hasNext()) {
      var element_0 = tmp$_0.next();
      if (element_0.faction === Faction$RES_getInstance())
        destination_0.add_11rb$(element_0);
    }
    this.smurfs = toSet(destination_0);
    this.allNonFaction = LinkedHashSet_init();
    this.allPortals = ArrayList_init_0();
  }
  Object.defineProperty(World.prototype, 'can', {
    get: function () {
      if (this.can_v8ttwa$_0 == null)
        return throwUPAE('can');
      return this.can_v8ttwa$_0;
    },
    set: function (can) {
      this.can_v8ttwa$_0 = can;
    }
  });
  World.prototype.ctx = function () {
    return HtmlUtil_getInstance().getContext2D_ap7jt0$(this.can);
  };
  Object.defineProperty(World.prototype, 'bgCan', {
    get: function () {
      if (this.bgCan_izup8r$_0 == null)
        return throwUPAE('bgCan');
      return this.bgCan_izup8r$_0;
    },
    set: function (bgCan) {
      this.bgCan_izup8r$_0 = bgCan;
    }
  });
  World.prototype.bgCtx = function () {
    return HtmlUtil_getInstance().getContext2D_ap7jt0$(this.bgCan);
  };
  Object.defineProperty(World.prototype, 'uiCan', {
    get: function () {
      if (this.uiCan_s0t3x6$_0 == null)
        return throwUPAE('uiCan');
      return this.uiCan_s0t3x6$_0;
    },
    set: function (uiCan) {
      this.uiCan_s0t3x6$_0 = uiCan;
    }
  });
  World.prototype.uiCtx = function () {
    return HtmlUtil_getInstance().getContext2D_ap7jt0$(this.uiCan);
  };
  World.prototype.resetAllCanvas = function () {
    clear(this.can);
    this.ctx().clearRect(0.0, 0.0, this.can.width, this.can.height);
    clear(this.bgCan);
    this.bgCtx().clearRect(0.0, 0.0, this.bgCan.width, this.bgCan.height);
    clear(this.uiCan);
    this.uiCtx().clearRect(0.0, 0.0, this.uiCan.width, this.uiCan.height);
  };
  World.prototype.w = function () {
    return this.can.width;
  };
  World.prototype.shadowW = function () {
    return this.w() / Coords$Companion_getInstance().res | 0;
  };
  World.prototype.h = function () {
    return this.can.height;
  };
  World.prototype.shadowH = function () {
    return this.h() / Coords$Companion_getInstance().res | 0;
  };
  World.prototype.diagonalLength = function () {
    var x = Kotlin.imul(this.can.width, this.can.width) + Kotlin.imul(this.can.height, this.can.height);
    return numberToInt(Math_0.sqrt(x));
  };
  World.prototype.totalArea = function () {
    return Kotlin.imul(this.can.width, this.can.height);
  };
  Object.defineProperty(World.prototype, 'noiseMap', {
    get: function () {
      if (this.noiseMap_ft1fdo$_0 == null)
        return throwUPAE('noiseMap');
      return this.noiseMap_ft1fdo$_0;
    },
    set: function (noiseMap) {
      this.noiseMap_ft1fdo$_0 = noiseMap;
    }
  });
  Object.defineProperty(World.prototype, 'noiseImage', {
    get: function () {
      if (this.noiseImage_c4tqbn$_0 == null)
        return throwUPAE('noiseImage');
      return this.noiseImage_c4tqbn$_0;
    },
    set: function (noiseImage) {
      this.noiseImage_c4tqbn$_0 = noiseImage;
    }
  });
  Object.defineProperty(World.prototype, 'grid', {
    get: function () {
      if (this.grid_pwdzco$_0 == null)
        return throwUPAE('grid');
      return this.grid_pwdzco$_0;
    },
    set: function (grid) {
      this.grid_pwdzco$_0 = grid;
    }
  });
  World.prototype.passableCells = function () {
    var $receiver = this.grid;
    var destination = LinkedHashMap_init();
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.value.isPassable) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    return destination;
  };
  World.prototype.wellPassableCells_0 = function () {
    var $receiver = this.grid;
    var destination = LinkedHashMap_init();
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.value.isPassableInAllDirections()) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    return destination;
  };
  World.prototype.passableOnScreen_0 = function () {
    var $receiver = this.wellPassableCells_0();
    var destination = LinkedHashMap_init();
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (!element.key.isOffGrid()) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    return destination;
  };
  World.prototype.passableInActionArea = function () {
    var $receiver = this.passableOnScreen_0();
    var destination = LinkedHashMap_init();
    var tmp$;
    tmp$ = $receiver.entries.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (ActionLimitsDisplay_getInstance().isNotBlocked_lfj9be$(element.key.fromShadow())) {
        destination.put_xwzc9p$(element.key, element.value);
      }
    }
    return destination;
  };
  World.prototype.countAgents = function () {
    return this.allAgents.size;
  };
  var checkCountOverflow = Kotlin.kotlin.collections.checkCountOverflow_za3lpa$;
  World.prototype.countAgents_bip15f$ = function (fact) {
    var $receiver = this.allAgents;
    var count$result;
    count$break: do {
      var tmp$;
      if (Kotlin.isType($receiver, Collection) && $receiver.isEmpty()) {
        count$result = 0;
        break count$break;
      }
      var count = 0;
      tmp$ = $receiver.iterator();
      while (tmp$.hasNext()) {
        var element = tmp$.next();
        if (element.faction === fact)
          checkCountOverflow((count = count + 1 | 0, count));
      }
      count$result = count;
    }
     while (false);
    return count$result;
  };
  World.prototype.canRecruitMore_bip15f$ = function (fact) {
    return this.countAgents_bip15f$(fact) < 21;
  };
  World.prototype.countNonFaction = function () {
    return this.allNonFaction.size;
  };
  World.prototype.randomPortal = function () {
    return this.allPortals.get_za3lpa$(numberToInt(Util_getInstance().random() * (World_getInstance().allPortals.size - 1 | 0)));
  };
  World.prototype.enlPortals = function () {
    var $receiver = this.allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var tmp$_0;
      if (equals((tmp$_0 = element.owner) != null ? tmp$_0.faction : null, Faction$ENL_getInstance()))
        destination.add_11rb$(element);
    }
    return destination;
  };
  World.prototype.resPortals = function () {
    var $receiver = this.allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var tmp$_0;
      if (equals((tmp$_0 = element.owner) != null ? tmp$_0.faction : null, Faction$RES_getInstance()))
        destination.add_11rb$(element);
    }
    return destination;
  };
  World.prototype.unclaimedPortals = function () {
    var $receiver = this.allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.owner == null)
        destination.add_11rb$(element);
    }
    return destination;
  };
  World.prototype.factionPortals_bip15f$ = function (fact) {
    var $receiver = this.allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var tmp$_0;
      if (equals((tmp$_0 = element.owner) != null ? tmp$_0.faction : null, fact))
        destination.add_11rb$(element);
    }
    return destination;
  };
  World.prototype.countPortals = function () {
    return this.allPortals.size;
  };
  World.prototype.countPortals_bip15f$ = function (fact) {
    return this.factionPortals_bip15f$(fact).size;
  };
  World.prototype.allLinks = function () {
    var $receiver = this.allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var list = element.links;
      addAll(destination, list);
    }
    return destination;
  };
  World.prototype.countLinks = function () {
    return this.allLinks().size;
  };
  World.prototype.countLinks_bip15f$ = function (fact) {
    var $receiver = this.allLinks();
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.creator.faction === fact)
        destination.add_11rb$(element);
    }
    return destination.size;
  };
  World.prototype.allFields = function () {
    var $receiver = this.allPortals;
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      var list = element.fields;
      addAll(destination, list);
    }
    return destination;
  };
  World.prototype.countFields = function () {
    return this.allFields().size;
  };
  World.prototype.countFields_bip15f$ = function (fact) {
    var $receiver = this.allFields();
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.owner.faction === fact)
        destination.add_11rb$(element);
    }
    return destination.size;
  };
  World.prototype.allLines = function () {
    var $receiver = this.allLinks();
    var destination = ArrayList_init(collectionSizeOrDefault($receiver, 10));
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var item = tmp$.next();
      destination.add_11rb$(item.getLine());
    }
    return destination;
  };
  World.prototype.calcTotalMu_bip15f$ = function (fact) {
    var $receiver = this.allFields();
    var destination = ArrayList_init_0();
    var tmp$;
    tmp$ = $receiver.iterator();
    while (tmp$.hasNext()) {
      var element = tmp$.next();
      if (element.owner.faction === fact)
        destination.add_11rb$(element);
    }
    var destination_0 = ArrayList_init(collectionSizeOrDefault(destination, 10));
    var tmp$_0;
    tmp$_0 = destination.iterator();
    while (tmp$_0.hasNext()) {
      var item = tmp$_0.next();
      destination_0.add_11rb$(item.calculateMu());
    }
    return sum(destination_0);
  };
  World.prototype.imageDataIndex_0 = function (x, y, w) {
    return (x + Kotlin.imul(y, w) | 0) * 4 | 0;
  };
  function World$createNoiseImage$setPixel(this$World, closure$alpha) {
    return function (imageData, x, y, r, g, b) {
      var index = this$World.imageDataIndex_0(x, y, imageData.width);
      imageData.data.set([toByte(r), toByte(b), toByte(g), toByte(numberToInt(kotlin_js_internal_ByteCompanionObject.MAX_VALUE * closure$alpha))], index);
    };
  }
  World.prototype.createNoiseImage_bd1o91$ = function (noiseMap, w, h, alpha) {
    if (alpha === void 0)
      alpha = 1.0;
    var setPixel = World$createNoiseImage$setPixel(this, alpha);
    var imageData = World_getInstance().bgCtx().createImageData(w, h);
    for (var x = 0; x < w; x++) {
      for (var y = 0; y < h; y++) {
        var rawNoise = noiseMap[x][y];
        var noisePoint = numberToInt((1 + -1 * rawNoise) * 0.5 * 127);
        setPixel(imageData, x, y, noisePoint, noisePoint, noisePoint);
      }
    }
    return imageData;
  };
  World.prototype.createStreetImage_n3vf4$ = function (streetMap, w, h) {
    var $receiver = World_getInstance();
    var imageData = $receiver.bgCtx().createImageData(w, h);
    for (var x = 0; x < w; x++) {
      for (var y = 0; y < h; y++) {
        var rawNoise = streetMap[$receiver.imageDataIndex_0(x, y, imageData.width)];
        var index = $receiver.imageDataIndex_0(x, h - 1 - y | 0, imageData.width);
        imageData.data.set([rawNoise, rawNoise, rawNoise, toByte(numberToInt(kotlin_js_internal_ByteCompanionObject.MAX_VALUE * 1.0))], index);
      }
    }
    return imageData;
  };
  function World$createNonFaction$lambda(closure$count, closure$callback, this$World) {
    return function () {
      if (closure$count > 0) {
        LoadingText_getInstance().draw_61zpoe$('Creating Non-Faction ' + closure$count);
        Loading$Companion_getInstance().draw();
        var newNonFaction = NonFaction$Companion_getInstance().create_5edep5$(World_getInstance().grid);
        World_getInstance().allNonFaction.add_11rb$(newNonFaction);
        this$World.createNonFaction_fzludj$(closure$callback, closure$count - 1 | 0);
      }
       else {
        closure$callback();
      }
    };
  }
  World.prototype.createNonFaction_fzludj$ = function (callback, count) {
    var tmp$;
    (tmp$ = document.defaultView) != null ? tmp$.setTimeout(World$createNonFaction$lambda(count, callback, this), 0) : null;
  };
  World.$metadata$ = {
    kind: Kind_OBJECT,
    simpleName: 'World',
    interfaces: []
  };
  var World_instance = null;
  function World_getInstance() {
    if (World_instance === null) {
      new World();
    }
    return World_instance;
  }
  Object.defineProperty(Action, 'Companion', {
    get: Action$Companion_getInstance
  });
  var package$agent = _.agent || (_.agent = {});
  var package$action = package$agent.action || (package$agent.action = {});
  package$action.Action = Action;
  Object.defineProperty(ActionItem, 'Companion', {
    get: ActionItem$Companion_getInstance
  });
  package$action.ActionItem = ActionItem;
  Object.defineProperty(package$action, 'ActionSelector', {
    get: ActionSelector_getInstance
  });
  var package$cond = package$action.cond || (package$action.cond = {});
  Object.defineProperty(package$cond, 'Attacker', {
    get: Attacker_getInstance
  });
  package$cond.ConditionalAction = ConditionalAction;
  Object.defineProperty(package$cond, 'Deployer', {
    get: Deployer_getInstance
  });
  Object.defineProperty(package$cond, 'Explorer', {
    get: Explorer_getInstance
  });
  Object.defineProperty(package$cond, 'Glypher', {
    get: Glypher_getInstance
  });
  Object.defineProperty(package$cond, 'Hacker', {
    get: Hacker_getInstance
  });
  Object.defineProperty(package$cond, 'Linker', {
    get: Linker_getInstance
  });
  Object.defineProperty(package$cond, 'Recharger', {
    get: Recharger_getInstance
  });
  Object.defineProperty(package$cond, 'Recruiter', {
    get: Recruiter_getInstance
  });
  Object.defineProperty(package$cond, 'Recycler', {
    get: Recycler_getInstance
  });
  Object.defineProperty(Agent, 'Companion', {
    get: Agent$Companion_getInstance
  });
  package$agent.Agent = Agent;
  Object.defineProperty(AgentSize, 'Companion', {
    get: AgentSize$Companion_getInstance
  });
  package$agent.AgentSize = AgentSize;
  Object.defineProperty(Faction, 'NONE', {
    get: Faction$NONE_getInstance
  });
  Object.defineProperty(Faction, 'ENL', {
    get: Faction$ENL_getInstance
  });
  Object.defineProperty(Faction, 'RES', {
    get: Faction$RES_getInstance
  });
  Object.defineProperty(Faction, 'Companion', {
    get: Faction$Companion_getInstance
  });
  package$agent.Faction = Faction;
  Object.defineProperty(Inventory, 'Companion', {
    get: Inventory$Companion_getInstance
  });
  package$agent.Inventory = Inventory;
  Object.defineProperty(package$agent, 'MovementUtil', {
    get: MovementUtil_getInstance
  });
  Object.defineProperty(NonFaction, 'Companion', {
    get: NonFaction$Companion_getInstance
  });
  package$agent.NonFaction = NonFaction;
  var package$qvalue = package$agent.qvalue || (package$agent.qvalue = {});
  Object.defineProperty(package$qvalue, 'QActions', {
    get: QActions_getInstance
  });
  Object.defineProperty(package$qvalue, 'QDestinations', {
    get: QDestinations_getInstance
  });
  package$qvalue.QValue = QValue;
  Object.defineProperty(Skills, 'Companion', {
    get: Skills$Companion_getInstance
  });
  package$agent.Skills = Skills;
  var package$config = _.config || (_.config = {});
  Object.defineProperty(package$config, 'Colors', {
    get: Colors_getInstance
  });
  Object.defineProperty(package$config, 'Config', {
    get: Config_getInstance
  });
  Object.defineProperty(package$config, 'Constants', {
    get: Constants_getInstance
  });
  Object.defineProperty(package$config, 'Dim', {
    get: Dim_getInstance
  });
  Object.defineProperty(Location, 'RED_SQUARE', {
    get: Location$RED_SQUARE_getInstance
  });
  Object.defineProperty(Location, 'RED_SQUARE_MOSCOW', {
    get: Location$RED_SQUARE_MOSCOW_getInstance
  });
  Object.defineProperty(Location, 'CHLOSER_PLATZ', {
    get: Location$CHLOSER_PLATZ_getInstance
  });
  Object.defineProperty(Location, 'GOLLUMS', {
    get: Location$GOLLUMS_getInstance
  });
  Object.defineProperty(Location, 'BAD_RAGAZ', {
    get: Location$BAD_RAGAZ_getInstance
  });
  Object.defineProperty(Location, 'ESCHER_WYSS', {
    get: Location$ESCHER_WYSS_getInstance
  });
  Object.defineProperty(Location, 'GIZA_PLATEAU', {
    get: Location$GIZA_PLATEAU_getInstance
  });
  Object.defineProperty(Location, 'EIFFEL_TOWER', {
    get: Location$EIFFEL_TOWER_getInstance
  });
  Object.defineProperty(Location, 'PRIME_TOWER', {
    get: Location$PRIME_TOWER_getInstance
  });
  Object.defineProperty(Location, 'GROUND_ZERO', {
    get: Location$GROUND_ZERO_getInstance
  });
  Object.defineProperty(Location, 'PLATZSPITZ', {
    get: Location$PLATZSPITZ_getInstance
  });
  Object.defineProperty(Location, 'Companion', {
    get: Location$Companion_getInstance
  });
  package$config.Location = Location;
  Object.defineProperty(package$config, 'OscillatorType', {
    get: OscillatorType_getInstance
  });
  Object.defineProperty(package$config, 'Probabilities', {
    get: Probabilities_getInstance
  });
  Object.defineProperty(package$config, 'Styles', {
    get: Styles_getInstance
  });
  Object.defineProperty(package$config, 'Time', {
    get: Time_getInstance
  });
  Object.defineProperty(VectorStyle, 'CIRCLE', {
    get: VectorStyle$CIRCLE_getInstance
  });
  Object.defineProperty(VectorStyle, 'SQUARE', {
    get: VectorStyle$SQUARE_getInstance
  });
  package$config.VectorStyle = VectorStyle;
  var package$items = _.items || (_.items = {});
  var package$deployable = package$items.deployable || (package$items.deployable = {});
  package$deployable.DeployableItem = DeployableItem;
  Object.defineProperty(LinkAmp, 'Companion', {
    get: LinkAmp$Companion_getInstance
  });
  package$deployable.LinkAmp = LinkAmp;
  Object.defineProperty(Multihack, 'Companion', {
    get: Multihack$Companion_getInstance
  });
  package$deployable.Multihack = Multihack;
  Object.defineProperty(Resonator, 'Companion', {
    get: Resonator$Companion_getInstance
  });
  package$deployable.Resonator = Resonator;
  package$deployable.Shield = Shield;
  package$deployable.Virus = Virus;
  var package$level = package$items.level || (package$items.level = {});
  package$level.ItemLevel = ItemLevel;
  Object.defineProperty(package$level, 'LevelColor', {
    get: LevelColor_getInstance
  });
  Object.defineProperty(PortalLevel, 'ZERO', {
    get: PortalLevel$ZERO_getInstance
  });
  Object.defineProperty(PortalLevel, 'ONE', {
    get: PortalLevel$ONE_getInstance
  });
  Object.defineProperty(PortalLevel, 'TWO', {
    get: PortalLevel$TWO_getInstance
  });
  Object.defineProperty(PortalLevel, 'THREE', {
    get: PortalLevel$THREE_getInstance
  });
  Object.defineProperty(PortalLevel, 'FOUR', {
    get: PortalLevel$FOUR_getInstance
  });
  Object.defineProperty(PortalLevel, 'FIVE', {
    get: PortalLevel$FIVE_getInstance
  });
  Object.defineProperty(PortalLevel, 'SIX', {
    get: PortalLevel$SIX_getInstance
  });
  Object.defineProperty(PortalLevel, 'SEVEN', {
    get: PortalLevel$SEVEN_getInstance
  });
  Object.defineProperty(PortalLevel, 'EIGHT', {
    get: PortalLevel$EIGHT_getInstance
  });
  Object.defineProperty(PortalLevel, 'Companion', {
    get: PortalLevel$Companion_getInstance
  });
  package$level.PortalLevel = PortalLevel;
  Object.defineProperty(PowerCubeLevel, 'ONE', {
    get: PowerCubeLevel$ONE_getInstance
  });
  Object.defineProperty(PowerCubeLevel, 'TWO', {
    get: PowerCubeLevel$TWO_getInstance
  });
  Object.defineProperty(PowerCubeLevel, 'THREE', {
    get: PowerCubeLevel$THREE_getInstance
  });
  Object.defineProperty(PowerCubeLevel, 'FOUR', {
    get: PowerCubeLevel$FOUR_getInstance
  });
  Object.defineProperty(PowerCubeLevel, 'FIVE', {
    get: PowerCubeLevel$FIVE_getInstance
  });
  Object.defineProperty(PowerCubeLevel, 'SIX', {
    get: PowerCubeLevel$SIX_getInstance
  });
  Object.defineProperty(PowerCubeLevel, 'SEVEN', {
    get: PowerCubeLevel$SEVEN_getInstance
  });
  Object.defineProperty(PowerCubeLevel, 'EIGHT', {
    get: PowerCubeLevel$EIGHT_getInstance
  });
  Object.defineProperty(PowerCubeLevel, 'Companion', {
    get: PowerCubeLevel$Companion_getInstance
  });
  package$level.PowerCubeLevel = PowerCubeLevel;
  Object.defineProperty(ResonatorLevel, 'ONE', {
    get: ResonatorLevel$ONE_getInstance
  });
  Object.defineProperty(ResonatorLevel, 'TWO', {
    get: ResonatorLevel$TWO_getInstance
  });
  Object.defineProperty(ResonatorLevel, 'THREE', {
    get: ResonatorLevel$THREE_getInstance
  });
  Object.defineProperty(ResonatorLevel, 'FOUR', {
    get: ResonatorLevel$FOUR_getInstance
  });
  Object.defineProperty(ResonatorLevel, 'FIVE', {
    get: ResonatorLevel$FIVE_getInstance
  });
  Object.defineProperty(ResonatorLevel, 'SIX', {
    get: ResonatorLevel$SIX_getInstance
  });
  Object.defineProperty(ResonatorLevel, 'SEVEN', {
    get: ResonatorLevel$SEVEN_getInstance
  });
  Object.defineProperty(ResonatorLevel, 'EIGHT', {
    get: ResonatorLevel$EIGHT_getInstance
  });
  Object.defineProperty(ResonatorLevel, 'Companion', {
    get: ResonatorLevel$Companion_getInstance
  });
  package$level.ResonatorLevel = ResonatorLevel;
  Object.defineProperty(UltraStrikeLevel, 'ONE', {
    get: UltraStrikeLevel$ONE_getInstance
  });
  Object.defineProperty(UltraStrikeLevel, 'TWO', {
    get: UltraStrikeLevel$TWO_getInstance
  });
  Object.defineProperty(UltraStrikeLevel, 'THREE', {
    get: UltraStrikeLevel$THREE_getInstance
  });
  Object.defineProperty(UltraStrikeLevel, 'FOUR', {
    get: UltraStrikeLevel$FOUR_getInstance
  });
  Object.defineProperty(UltraStrikeLevel, 'FIVE', {
    get: UltraStrikeLevel$FIVE_getInstance
  });
  Object.defineProperty(UltraStrikeLevel, 'SIX', {
    get: UltraStrikeLevel$SIX_getInstance
  });
  Object.defineProperty(UltraStrikeLevel, 'SEVEN', {
    get: UltraStrikeLevel$SEVEN_getInstance
  });
  Object.defineProperty(UltraStrikeLevel, 'EIGHT', {
    get: UltraStrikeLevel$EIGHT_getInstance
  });
  Object.defineProperty(UltraStrikeLevel, 'Companion', {
    get: UltraStrikeLevel$Companion_getInstance
  });
  package$level.UltraStrikeLevel = UltraStrikeLevel;
  Object.defineProperty(XmpLevel, 'ONE', {
    get: XmpLevel$ONE_getInstance
  });
  Object.defineProperty(XmpLevel, 'TWO', {
    get: XmpLevel$TWO_getInstance
  });
  Object.defineProperty(XmpLevel, 'THREE', {
    get: XmpLevel$THREE_getInstance
  });
  Object.defineProperty(XmpLevel, 'FOUR', {
    get: XmpLevel$FOUR_getInstance
  });
  Object.defineProperty(XmpLevel, 'FIVE', {
    get: XmpLevel$FIVE_getInstance
  });
  Object.defineProperty(XmpLevel, 'SIX', {
    get: XmpLevel$SIX_getInstance
  });
  Object.defineProperty(XmpLevel, 'SEVEN', {
    get: XmpLevel$SEVEN_getInstance
  });
  Object.defineProperty(XmpLevel, 'EIGHT', {
    get: XmpLevel$EIGHT_getInstance
  });
  Object.defineProperty(XmpLevel, 'Companion', {
    get: XmpLevel$Companion_getInstance
  });
  package$level.XmpLevel = XmpLevel;
  Object.defineProperty(PowerCube, 'Companion', {
    get: PowerCube$Companion_getInstance
  });
  package$items.PowerCube = PowerCube;
  package$items.QgressItem = QgressItem;
  Object.defineProperty(LinkAmpType, 'RARE', {
    get: LinkAmpType$RARE_getInstance
  });
  Object.defineProperty(LinkAmpType, 'VERY_RARE', {
    get: LinkAmpType$VERY_RARE_getInstance
  });
  var package$types = package$items.types || (package$items.types = {});
  package$types.LinkAmpType = LinkAmpType;
  Object.defineProperty(ModType, 'RES_SHIELD', {
    get: ModType$RES_SHIELD_getInstance
  });
  Object.defineProperty(ModType, 'MULTIHACK', {
    get: ModType$MULTIHACK_getInstance
  });
  Object.defineProperty(ModType, 'FORCE_AMP', {
    get: ModType$FORCE_AMP_getInstance
  });
  Object.defineProperty(ModType, 'HEATSINK', {
    get: ModType$HEATSINK_getInstance
  });
  Object.defineProperty(ModType, 'TURRET', {
    get: ModType$TURRET_getInstance
  });
  Object.defineProperty(ModType, 'LINK_AMPLIFIER', {
    get: ModType$LINK_AMPLIFIER_getInstance
  });
  package$types.ModType = ModType;
  Object.defineProperty(MultihackType, 'COMMON', {
    get: MultihackType$COMMON_getInstance
  });
  Object.defineProperty(MultihackType, 'RARE', {
    get: MultihackType$RARE_getInstance
  });
  Object.defineProperty(MultihackType, 'VERY_RARE', {
    get: MultihackType$VERY_RARE_getInstance
  });
  package$types.MultihackType = MultihackType;
  Object.defineProperty(ShieldType, 'COMMON', {
    get: ShieldType$COMMON_getInstance
  });
  Object.defineProperty(ShieldType, 'RARE', {
    get: ShieldType$RARE_getInstance
  });
  Object.defineProperty(ShieldType, 'VERY_RARE', {
    get: ShieldType$VERY_RARE_getInstance
  });
  Object.defineProperty(ShieldType, 'AEGIS', {
    get: ShieldType$AEGIS_getInstance
  });
  Object.defineProperty(ShieldType, 'Companion', {
    get: ShieldType$Companion_getInstance
  });
  package$types.ShieldType = ShieldType;
  Object.defineProperty(VirusType, 'JARVIS_VIRUS', {
    get: VirusType$JARVIS_VIRUS_getInstance
  });
  Object.defineProperty(VirusType, 'ADA_REFACTOR', {
    get: VirusType$ADA_REFACTOR_getInstance
  });
  package$types.VirusType = VirusType;
  package$items.UltraStrike = UltraStrike;
  Object.defineProperty(XmpBurster, 'Companion', {
    get: XmpBurster$Companion_getInstance
  });
  package$items.XmpBurster = XmpBurster;
  _.main_kand9s$ = main;
  Object.defineProperty(Cooldown, 'BURNOUT', {
    get: Cooldown$BURNOUT_getInstance
  });
  Object.defineProperty(Cooldown, 'FIVE', {
    get: Cooldown$FIVE_getInstance
  });
  Object.defineProperty(Cooldown, 'THREE', {
    get: Cooldown$THREE_getInstance
  });
  Object.defineProperty(Cooldown, 'TWO', {
    get: Cooldown$TWO_getInstance
  });
  Object.defineProperty(Cooldown, 'ONE', {
    get: Cooldown$ONE_getInstance
  });
  Object.defineProperty(Cooldown, 'HALF', {
    get: Cooldown$HALF_getInstance
  });
  Object.defineProperty(Cooldown, 'MIN', {
    get: Cooldown$MIN_getInstance
  });
  Object.defineProperty(Cooldown, 'NONE', {
    get: Cooldown$NONE_getInstance
  });
  Object.defineProperty(Cooldown, 'Companion', {
    get: Cooldown$Companion_getInstance
  });
  var package$portal = _.portal || (_.portal = {});
  package$portal.Cooldown = Cooldown;
  Object.defineProperty(Field, 'Companion', {
    get: Field$Companion_getInstance
  });
  package$portal.Field = Field;
  package$portal.HackResult = HackResult;
  Object.defineProperty(Link, 'Companion', {
    get: Link$Companion_getInstance
  });
  package$portal.Link = Link;
  package$portal.LinkResult = LinkResult;
  Object.defineProperty(ModSlot, 'FIRST', {
    get: ModSlot$FIRST_getInstance
  });
  Object.defineProperty(ModSlot, 'SECOND', {
    get: ModSlot$SECOND_getInstance
  });
  Object.defineProperty(ModSlot, 'THIRD', {
    get: ModSlot$THIRD_getInstance
  });
  Object.defineProperty(ModSlot, 'FOURTH', {
    get: ModSlot$FOURTH_getInstance
  });
  package$portal.ModSlot = ModSlot;
  Object.defineProperty(Octant, 'E', {
    get: Octant$E_getInstance
  });
  Object.defineProperty(Octant, 'SE', {
    get: Octant$SE_getInstance
  });
  Object.defineProperty(Octant, 'S', {
    get: Octant$S_getInstance
  });
  Object.defineProperty(Octant, 'SW', {
    get: Octant$SW_getInstance
  });
  Object.defineProperty(Octant, 'W', {
    get: Octant$W_getInstance
  });
  Object.defineProperty(Octant, 'NW', {
    get: Octant$NW_getInstance
  });
  Object.defineProperty(Octant, 'N', {
    get: Octant$N_getInstance
  });
  Object.defineProperty(Octant, 'NE', {
    get: Octant$NE_getInstance
  });
  package$portal.Octant = Octant;
  Object.defineProperty(Portal, 'Companion', {
    get: Portal$Companion_getInstance
  });
  package$portal.Portal = Portal;
  Object.defineProperty(PortalKey, 'Companion', {
    get: PortalKey$Companion_getInstance
  });
  package$portal.PortalKey = PortalKey;
  Object.defineProperty(Quality, 'BEST', {
    get: Quality$BEST_getInstance
  });
  Object.defineProperty(Quality, 'TOP', {
    get: Quality$TOP_getInstance
  });
  Object.defineProperty(Quality, 'GOOD', {
    get: Quality$GOOD_getInstance
  });
  Object.defineProperty(Quality, 'MORE', {
    get: Quality$MORE_getInstance
  });
  package$portal.Quality = Quality;
  Object.defineProperty(ResonatorSlot, 'Companion', {
    get: ResonatorSlot$Companion_getInstance
  });
  package$portal.ResonatorSlot = ResonatorSlot;
  Object.defineProperty(XmHeap, 'Companion', {
    get: XmHeap$Companion_getInstance
  });
  package$portal.XmHeap = XmHeap;
  Object.defineProperty(package$portal, 'XmMap', {
    get: XmMap_getInstance
  });
  Object.defineProperty(Checkpoint, 'Companion', {
    get: Checkpoint$Companion_getInstance
  });
  var package$system = _.system || (_.system = {});
  package$system.Checkpoint = Checkpoint;
  Object.defineProperty(package$system, 'Com', {
    get: Com_getInstance
  });
  Object.defineProperty(Cycle, 'INSTANCE', {
    get: Cycle$INSTANCE_getInstance
  });
  Object.defineProperty(Cycle, 'Companion', {
    get: Cycle$Companion_getInstance
  });
  package$system.Cycle = Cycle;
  var package$display = package$system.display || (package$system.display = {});
  Object.defineProperty(package$display, 'Attacks', {
    get: Attacks_getInstance
  });
  package$display.Display = Display;
  Object.defineProperty(Loading, 'Companion', {
    get: Loading$Companion_getInstance
  });
  var package$loading = package$display.loading || (package$display.loading = {});
  package$loading.Loading = Loading;
  Object.defineProperty(package$loading, 'LoadingText', {
    get: LoadingText_getInstance
  });
  Object.defineProperty(package$loading, 'NpcBar', {
    get: NpcBar_getInstance
  });
  Object.defineProperty(package$loading, 'VectorBar', {
    get: VectorBar_getInstance
  });
  var package$ui = package$display.ui || (package$display.ui = {});
  Object.defineProperty(package$ui, 'ActionLimitsDisplay', {
    get: ActionLimitsDisplay_getInstance
  });
  Object.defineProperty(package$ui, 'CycleDisplay', {
    get: CycleDisplay_getInstance
  });
  Object.defineProperty(package$ui, 'MindUnits', {
    get: MindUnits_getInstance
  });
  Object.defineProperty(package$ui, 'StatsDisplay', {
    get: StatsDisplay_getInstance
  });
  var package$table = package$ui.table || (package$ui.table = {});
  Object.defineProperty(package$table, 'TopAgentsDisplay', {
    get: TopAgentsDisplay_getInstance
  });
  package$table.UiTable = UiTable;
  Object.defineProperty(package$display, 'TickDisplay', {
    get: TickDisplay_getInstance
  });
  VectorFields.prototype.VecKey = VectorFields$VecKey;
  Object.defineProperty(package$display, 'VectorFields', {
    get: VectorFields_getInstance
  });
  Object.defineProperty(package$system, 'Queues', {
    get: Queues_getInstance
  });
  var package$util = _.util || (_.util = {});
  Object.defineProperty(package$util, 'ColorUtil', {
    get: ColorUtil_getInstance
  });
  var package$data = package$util.data || (package$util.data = {});
  package$data.Cell = Cell;
  package$data.Circle = Circle;
  Object.defineProperty(Complex, 'Companion', {
    get: Complex$Companion_getInstance
  });
  package$data.Complex_init_vux9f0$ = Complex_init;
  package$data.Complex = Complex;
  Object.defineProperty(Coords, 'Companion', {
    get: Coords$Companion_getInstance
  });
  package$data.Coords_init_vux9f0$ = Coords_init;
  package$data.Coords = Coords;
  package$data.Damage = Damage;
  package$data.Dim = Dim_0;
  package$data.GeoCircle = GeoCircle;
  Object.defineProperty(GeoCoords, 'Companion', {
    get: GeoCoords$Companion_getInstance
  });
  package$data.GeoCoords = GeoCoords;
  package$data.GeoLine = GeoLine;
  Object.defineProperty(AgentsTableWidget, 'Companion', {
    get: AgentsTableWidget$Companion_getInstance
  });
  var package$html = package$data.html || (package$data.html = {});
  package$html.AgentsTableWidget = AgentsTableWidget;
  Object.defineProperty(Line, 'Companion', {
    get: Line$Companion_getInstance
  });
  package$data.Line = Line;
  Object.defineProperty(package$util, 'DrawUtil', {
    get: DrawUtil_getInstance
  });
  Object.defineProperty(package$util, 'HtmlUtil', {
    get: HtmlUtil_getInstance
  });
  Object.defineProperty(_, 'ImprovedNoise', {
    get: ImprovedNoise_getInstance
  });
  Object.defineProperty(package$util, 'MapUtil', {
    get: MapUtil_getInstance
  });
  Object.defineProperty(package$util, 'PathUtil', {
    get: PathUtil_getInstance
  });
  Object.defineProperty(package$util, 'SoundUtil', {
    get: SoundUtil_getInstance
  });
  Object.defineProperty(package$util, 'Util', {
    get: Util_getInstance
  });
  Object.defineProperty(_, 'World', {
    get: World_getInstance
  });
  main([]);
  Kotlin.defineModule('Q-Gress', _);
  return _;
}));

//# sourceMappingURL=Q-Gress.js.map
