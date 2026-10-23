package net.rossonet.waldot.rules.commands;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * Show case "Dungeon Arena": a tower defense game driven by the rules engine.
 * <p>
 * Three generators (waves of enemies, mana of the tower, turn clock) feed the
 * state vertices with <code>link-to</code> edges. Seven rules react with
 * <code>fire</code> edges and change the state of the game: the tower casts
 * spells, takes damage, the player levels up, and when the tower falls the
 * game restarts. Two compute vertices run the rules with different thread
 * counts and priorities. One rule is broken on purpose to show the
 * <code>errors</code> counter.
 * </p>
 * Input argument: <code>delay-ms</code> (period of the generators, default
 * 250).
 */
public class CreateShowCaseArenaCommand extends AbstractShowCaseCommand {

	public static final String COMBAT_COMPUTE_ID = "arena-combat";
	public static final String DEFAULT_DELAY_LABEL = "delay-ms";
	public static final long DEFAULT_DELAY_MS = 250;
	public static final String ECONOMY_COMPUTE_ID = "arena-economy";
	public static final String PLAYER_ID = "arena-player";
	/** The existence of this vertex marks the scenario as already created. */
	public static final String TOWER_ID = "arena-tower";
	public static final String TURN_ID = "arena-turn";
	public static final String WAVE_ID = "arena-wave";

	private static final String DIR = BASE_DIRECTORY + "/arena";

	public CreateShowCaseArenaCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace, "create show case arena",
				"creates the Dungeon Arena example: a tower defense game driven by generators and rules");
		addStringArgument(DEFAULT_DELAY_LABEL,
				"period in milliseconds of the generators (default "
						+ DEFAULT_DELAY_MS + ", minimum 10)");
	}

	@Override
	public Object clone() {
		return new CreateShowCaseArenaCommand(this.waldotNamespace);
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext,
			final String[] inputValues) {
		if (exists(TOWER_ID)) {
			return new String[]{
					"show case arena already exists: remove its vertices before creating it again"};
		}
		final long delay;
		try {
			delay = longArgument(inputValues, 0, DEFAULT_DELAY_MS,
					DEFAULT_DELAY_LABEL);
		} catch (final IllegalArgumentException e) {
			return new String[]{e.getMessage()};
		}
		if (delay < 10) {
			return new String[]{"argument '" + DEFAULT_DELAY_LABEL
					+ "' must be at least 10"};
		}

		// game state (all numeric values are doubles)
		final Vertex tower = vertex(TOWER_ID, null, "Tower",
				DIR + "/state", "data", 0.0, "hp", 100.0, "kills", 0.0,
				"spells", 0.0);
		final Vertex wave = vertex(WAVE_ID, null, "Wave",
				DIR + "/state", "data", 0.0, "difficulty", 1.0);
		final Vertex player = vertex(PLAYER_ID, null, "Player",
				DIR + "/state", "level", 1.0, "gold", 0.0, "score", 0.0,
				"defeats", 0.0);
		final Vertex turn = vertex(TURN_ID, null, "Turn",
				DIR + "/state", "data", 0.0);

		// compute vertices
		final Vertex combat = compute(COMBAT_COMPUTE_ID, "Combat compute",
				DIR + "/compute", 4);
		final Vertex economy = compute(ECONOMY_COMPUTE_ID, "Economy compute",
				DIR + "/compute", 1);

		// rules
		final String waveData = val(WAVE_ID, "data");
		final String towerHp = val(TOWER_ID, "hp");
		final String kills = val(TOWER_ID, "kills");
		final String level = val(PLAYER_ID, "level");
		final String difficulty = val(WAVE_ID, "difficulty");

		final Vertex spawnWave = rule("arena-rule-spawn-wave", "Spawn wave",
				DIR + "/rules",
				waveData + " > 70.0 && " + difficulty + " < 10.0",
				set(WAVE_ID, "difficulty", difficulty + " + 1.0"), 0, 0);
		final Vertex fireSpell = rule("arena-rule-fire-spell", "Fire spell",
				DIR + "/rules",
				val(TOWER_ID, "data") + " > 80.0 && " + waveData + " > 20.0",
				set(TOWER_ID, "kills", kills + " + 1.0") + "; "
						+ set(TOWER_ID, "spells",
								val(TOWER_ID, "spells") + " + 1.0"),
				300, 0);
		final Vertex towerDamage = rule("arena-rule-tower-damage",
				"Tower damage", DIR + "/rules",
				waveData + " > 50.0 + 5.0 * " + level,
				set(TOWER_ID, "hp",
						"math.max(0.0, " + towerHp + " - 1.0 - " + difficulty
								+ " / 2.0)"),
				200, 0);
		final Vertex levelUp = rule("arena-rule-level-up", "Level up",
				DIR + "/rules", kills + " > 0.0 && " + kills + " % 5.0 == 0.0",
				set(PLAYER_ID, "level", level + " + 1.0") + "; "
						+ set(PLAYER_ID, "gold",
								val(PLAYER_ID, "gold") + " + 10.0 * " + level),
				0, 1);
		final Vertex gameOver = rule("arena-rule-game-over", "Game over",
				DIR + "/rules", towerHp + " <= 0.0",
				set(PLAYER_ID, "defeats", val(PLAYER_ID, "defeats") + " + 1.0")
						+ "; " + set(TOWER_ID, "hp", "100.0") + "; "
						+ set(TOWER_ID, "kills", "0.0") + "; "
						+ set(WAVE_ID, "difficulty", "1.0") + "; "
						+ set(PLAYER_ID, "level", "1.0"),
				0, 0);
		final Vertex turnTick = rule("arena-rule-turn-tick", "Turn tick",
				DIR + "/rules", val(TURN_ID, "data") + " % 10.0 == 0.0",
				set(PLAYER_ID, "score", val(PLAYER_ID, "score") + " + 10.0 * "
						+ level),
				0, 0);
		// broken on purpose: the condition always fails, the 'errors' counter grows
		final Vertex broken = rule("arena-rule-broken", "Broken rule (demo)",
				DIR + "/rules", "1 / 0 > 0", "log.info('never executed')", 0,
				0);

		// execution: who runs which rule, with which priority
		execute(combat, fireSpell, "runs-fire-spell", 200, DIR + "/compute");
		execute(combat, towerDamage, "runs-tower-damage", 150,
				DIR + "/compute");
		execute(combat, spawnWave, "runs-spawn-wave", 100, DIR + "/compute");
		execute(combat, broken, "runs-broken", 10, DIR + "/compute");
		execute(economy, gameOver, "runs-game-over", 200, DIR + "/compute");
		execute(economy, levelUp, "runs-level-up", 100, DIR + "/compute");
		execute(economy, turnTick, "runs-turn-tick", 50, DIR + "/compute");

		// triggers: which change fires which rule
		fire(wave, spawnWave, "wave-spawns", "data", DIR + "/triggers");
		fire(wave, towerDamage, "wave-hits-tower", "data", DIR + "/triggers");
		fire(tower, fireSpell, "mana-casts-spell", "data", DIR + "/triggers");
		fire(tower, levelUp, "kills-level-up", "kills", DIR + "/triggers");
		fire(tower, gameOver, "hp-game-over", "hp", DIR + "/triggers");
		fire(turn, turnTick, "turn-scores", "data", DIR + "/triggers");
		fire(turn, broken, "turn-breaks", "data", DIR + "/triggers");

		// generators feed the game state
		final Vertex waveGenerator = generator("arena-gen-wave",
				"Enemy waves", DIR + "/generators", "random", 0, 100, delay);
		final Vertex manaGenerator = generator("arena-gen-mana",
				"Tower mana", DIR + "/generators", "sinusoidal", 0, 100,
				delay);
		final Vertex turnGenerator = generator("arena-gen-turn",
				"Turn clock", DIR + "/generators", "incremental", 0, 1000000,
				delay * 2);
		linkTo(waveGenerator, wave, "enemies-to-wave", "data",
				DIR + "/generators");
		linkTo(manaGenerator, tower, "mana-to-tower", "data",
				DIR + "/generators");
		linkTo(turnGenerator, turn, "clock-to-turn", "data",
				DIR + "/generators");

		return new String[]{
				"show case arena created: 3 generators, 4 state vertices, 2 compute, 7 rules"};
	}
}
