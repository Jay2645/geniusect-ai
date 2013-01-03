/*
 * A bunch of game constants, formulas, and the like.
 * @author TeamForretress
 */

package geniusect;
import java.awt.Point;

public class Pokequations {
	
	public static Point calculateDamagePercent(Pokemon attacker, Move move, Pokemon defender)
	{
		if(move.getType() == MoveType.Status)
			return new Point(0,0);
		Point percentage = calculateDamage(attacker, move, defender);
		if(defender.getFullHP() == 0)
			defender.query();
		if(defender.getFullHP() == 0) //There's something wrong with the SQL data.
			return new Point(1,1);
		double fullHP = (double)defender.getFullHP();
		double percentageX = (percentage.x / fullHP) * 100;
		double percentageY = (percentage.y / fullHP) * 100;
		percentage.x = (int)Math.round(percentageX);
		percentage.y = (int)Math.round(percentageY);
		return percentage;
	}
	public static Point calculateDamage(Spread attacker, Move move, Spread defender)
	{
		//Make sure we can't use the exact calculations unless we believe we know everything about both sides.
		return calculateDamage(attacker,move,defender);
	}
	
	public static Point calculateDamage(Pokemon attacker, Move move, Pokemon defender)
	{
		//System.out.println("Calculating the damage if "+attacker.name+" uses "+move.name+" on "+defender.name);
		//Returns damage dealt as a point(minValue, maxValue).
		if(move.getType() == MoveType.Status)
			return new Point(0,0);
		Type[] immunities = defender.getImmunities();
		for(int i = 0; i < immunities.length; i++)
		{
			if(immunities[i] == move.type)
				return new Point(0,0);
		}
		double stab = 1;
		if(attacker.getType(0) == move.type || attacker.getType(1) == move.type)
			stab = attacker.getSTAB();
		int attackPower = move.power;
		int attackStat;
		int defenseStat;
		int level = attacker.getLevel();
		if(move.getType() == MoveType.Special)
		{
			attackStat = attacker.boostedStat(Stat.SpA);
			defenseStat = defender.boostedStat(Stat.SpD);
		}
		else
		{
			attackStat = attacker.boostedStat(Stat.Atk);
			defenseStat = defender.boostedStat(Stat.Def);
		}
		if(defender.boostedStat(Stat.Def) == 0)
			defenseStat = 100; //Means we could not look up this Pokemon's defense stat for some reason.
		//TODO: Convert Hidden Power to correct type.
		double multiplier = damageMultiplier(move.type, defender.getTypes());
		
		return calculateDamage(level, attackStat, attackPower, defenseStat, stab, multiplier,attacker.getAbilityModifier());
	}
	
	private static Point calculateDamage(int level, int attackStat, int attackPower, int defenseStat, double stab, double multiplier, double modifier)
	{
		if(defenseStat == 0)
			return new Point(1,1);
		//Returns damage dealt as a point(minValue, maxValue).
		
		Point p = new Point();
		p.y = (int)Math.floor(((((2 * level / 5 + 2) * attackStat * (attackPower * modifier) / defenseStat) / 50) + 2) * stab * multiplier);
		p.x = (int)Math.ceil(p.y * 0.85);
		//System.out.println("Max damage is "+p.y);
		return p;
	}
	
	
	
	public static int calculateAtkStat(Pokemon attacker, Move move, Pokemon defender, int percentageLost)
	{
		int attackPower = move.power;
		int level = attacker.getLevel();
		int defenseStat;
		if(move.getType() == MoveType.Special)
			defenseStat = defender.boostedStat(Stat.SpD);
		else
			defenseStat = defender.boostedStat(Stat.Def);
		double multiplier = damageMultiplier(move.type, defender.getTypes());
		double bonus = 1;
		if(attacker.getType(0) == move.type || attacker.getType(1) == move.type)
			bonus = attacker.getSTAB();
		int damage = calculateHPDamage(percentageLost,defender.boostedStat(Stat.HP));
		
		return (int)Math.floor(50 * damage * defenseStat / (bonus * multiplier * attackPower * (2 * level / 5 + 2)) - 100 * defenseStat / (attackPower * (2 * level / 5 + 2)));
	}
	
	public static int calculateDefStat(Pokemon attacker, Move move, Pokemon defender, int percentageLost)
	{
		/*int attackPower = move.power;
		int level = attacker.level;
		int defenseStat;
		if(move.special)
			defenseStat = defender.stats[4];
		else
			defenseStat = defender.stats[2];
		double multiplier = damageMultiplier(move.type, defender.types);
		double bonus = 1;
		if(attacker.types[0] == move.type || attacker.types[1] == move.type)
			bonus = 1.5;
		int damage = calculateHPDamage(percentageLost,defender.stats[0]);
		return (int)Math.floor(50 * damage * defenseStat / (bonus * multiplier * attackPower * (2 * level / 5 + 2)) - 100 * defenseStat / (attackPower * (2 * level / 5 + 2)));
		*/
		//TODO: Work out what to do about this.
		return 0;
	}
	
	public static int calculateHPDamage(int percentage, int hp)
	{
		//Returns the amount of HP lost based upon our known HP value and a percentage of lost HP.
		return Math.round((percentage / 100) * hp);
	}
	
	public static double damageMultiplier(Type move, Type[] enemy)
	{
		if(move == Type.None || move == null)
			return 1;
		//Returns the damage multiplier value for a type matchup.
		double first = SQLHandler.queryDamage(move, enemy[0]);
		if(enemy[1] == Type.None)
			return first;
		double second = SQLHandler.queryDamage(move, enemy[1]);
		return first * second;
	}
	
	public static int[] calculateStat(Pokemon pokemon)
	{
		int[] stats = new int[6];
		for(int i = 0; i < 6; i++)
		{
			stats[i] = calculateStat(Stat.fromInt(i),pokemon);
		}
		return stats;
	}
	
	public static int calculateStat(Stat type, Pokemon pokemon)
	{
		if(pokemon.getBaseStat(type) == 0)
			return 0;
		return calculateStat(type, pokemon.getNature(), pokemon.getBaseStat(type), pokemon.getIVs(type),pokemon.getEVs(type), pokemon.getLevel());
	}
	
	public static int calculateStat(Stat type, Nature nature, int base, int iv, int ev, int level)
	{
		return calculateStat(type,nature.multiplier(type),base,iv,ev,level);
	}
	
	public static int calculateStat(Stat type, double natureValue, int base, int iv, int ev, int level)
	{
		//Returns any non-HP stat as an int.
		if(type == Stat.HP)
			return calculateHP(base,iv,ev,level);
		else return (int) Math.ceil((((iv + 2 * base + (ev/4) ) * level/100 ) + 5) * natureValue);
	}
	public static int calculateHP(int base, int iv, int ev, int level)
	{
		//Returns the HP stat as an int.
		return (int) Math.ceil(((iv + 2 * base + (ev/4) ) * level/100 ) + 10 + level);
	}
	
	public static int calculateEVs(Stat stat, Pokemon pokemon)
	{
		return calculateEVs(pokemon.getNature(),stat,pokemon.getBaseStat(stat),pokemon.getLevel(),pokemon.getStats(stat),pokemon.getIVs(stat));
	}
	
	public static int calculateEVs(Nature nature, Stat stat, int base, int level, int statValue, int iv)
	{
		double natureValue = nature.multiplier(stat);
		return (int)Math.ceil(-(4 *(natureValue * (2 * base*level+level * iv+500)-100 * statValue))/(natureValue * level));
	}
	
	public static int statBoost(int level, int stat)
	{
		//Adjusts a stat for a certain number of boosts, then returns the adjusted stat.
		double adjust = 1;
		if(level < -6)
			level = -6;
		else if(level > 6)
			level = 6;
		switch(level) {
			case -6:	adjust = 0.25;
						break;
			case -5:	adjust = 0.285;
						break;
			case -4:	adjust = 0.33;
						break;
			case -3:	adjust = 0.4;
						break;
			case -2:	adjust = 0.5;
						break;
			case -1:	adjust = 0.66;
						break;
			case 0:		adjust = 1;
						break;
			case 1:		adjust = 1.5;
						break;
			case 2:		adjust = 2;
						break;
			case 3:		adjust = 2.5;
						break;
			case 4:		adjust = 3;
						break;
			case 5:		adjust = 3.5;
						break;
			case 6:		adjust = 4;
						break;
		}
		return (int)Math.round(stat*adjust);
	}
	
	public static Move bestMove(Pokemon attacker, Pokemon defender, Move enemyMove)
	{
		//Proper calculation for Wobbuffet, Wynaut, etc.
		boolean foundMove = false;
		Move[] moveset = attacker.getMoveset();
		for(int i = 0; i < moveset.length; i++)
		{
			if(moveset[i] == null || moveset[i].disabled)
				continue;
			foundMove = true;
			int projectedDamageFromEnemy = 0;
			if(enemyMove.getProjectedPercent(attacker) == null)
				projectedDamageFromEnemy = calculateDamagePercent(attacker,enemyMove,defender).y;
			else
				projectedDamageFromEnemy = enemyMove.getProjectedPercent(attacker).y;
			int turnsUntilDead = turnsToKill(attacker.getHealth(), projectedDamageFromEnemy + moveset[i].recoilPercent);
			if(turnsUntilDead > 1)
			{
				if(moveset[i].name.toLowerCase().startsWith("counter") && enemyMove.getType() == MoveType.Physical || moveset[i].name.toLowerCase().startsWith("mirror coat") && enemyMove.getType() == MoveType.Special)
				{
					int projectedDamageLower = enemyMove.getProjectedDamage(attacker).x * 2;
					int projectedDamageUpper = enemyMove.getProjectedDamage(attacker).y * 2;
					moveset[i].getProjectedDamage(defender).x = projectedDamageLower;
					moveset[i].getProjectedDamage(defender).y = projectedDamageUpper;
					moveset[i].getProjectedPercent(defender).x = defender.hpToPercent(projectedDamageLower);
					moveset[i].getProjectedPercent(defender).y = defender.hpToPercent(projectedDamageUpper);
				}
				else if(moveset[i].name.toLowerCase().startsWith("counter") && enemyMove.getType() != MoveType.Physical || moveset[i].name.toLowerCase().startsWith("mirror coat") && enemyMove.getType() != MoveType.Special)
				{
					moveset[i].getProjectedDamage(defender).x = 0;
					moveset[i].getProjectedDamage(defender).y = 0;
					moveset[i].getProjectedPercent(defender).x = 0;
					moveset[i].getProjectedPercent(defender).y = 0;
				}
				if(turnsUntilDead == 2 && !attacker.isFasterThan(defender))
				{
					if(moveset[i].name.toLowerCase().startsWith("destiny bond"))
					{
						moveset[i].getProjectedDamage(defender).x = Integer.MAX_VALUE - 1;
						moveset[i].getProjectedDamage(defender).y = Integer.MAX_VALUE;
						moveset[i].getProjectedPercent(defender).x = 99;
						moveset[i].getProjectedPercent(defender).y = 100;
					}
				}
				else if(moveset[i].name.toLowerCase().startsWith("destiny bond"))
				{
					moveset[i].getProjectedDamage(defender).x = 0;
					moveset[i].getProjectedDamage(defender).y = 0;
					moveset[i].getProjectedPercent(defender).x = 0;
					moveset[i].getProjectedPercent(defender).y = 0;
				}
			}
			else
			{
				if(moveset[i].name.toLowerCase().startsWith("counter")|| moveset[i].name.toLowerCase().startsWith("mirror coat"))
				{
					moveset[i].getProjectedDamage(defender).x = 0;
					moveset[i].getProjectedDamage(defender).y = 0;
					moveset[i].getProjectedPercent(defender).x = 0;
					moveset[i].getProjectedPercent(defender).y = 0;
				}
				else if(turnsUntilDead == 1 && attacker.isFasterThan(defender) && moveset[i].name.toLowerCase().startsWith("destiny bond"))
				{
					moveset[i].getProjectedDamage(defender).x = Integer.MAX_VALUE - 1;
					moveset[i].getProjectedDamage(defender).y = Integer.MAX_VALUE;
					moveset[i].getProjectedPercent(defender).x = 99;
					moveset[i].getProjectedPercent(defender).y = 100;
				}
			}
		}
		if(foundMove)
			return bestMove(attacker,defender);
		else return new Move("Struggle", attacker);
	}
	
	public static Move bestMove(Pokemon attacker, Pokemon defender)
	{
		return bestMove(attacker, defender, attacker.getMoveset());
	}
	
	public static Move bestMove(Pokemon attacker, Pokemon defender, Move[] moveset)
	{
		if(attacker.getLockedInto() != null)
			return attacker.getLockedInto();
		Point damage = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE + 1);
		Move use = null;
		for(int i = 0; i < moveset.length; i++)
		{
			if(moveset[i] == null || moveset[i].disabled || moveset[i].pp <= 0)
			{
				System.err.println(attacker.getName()+"'s move "+moveset[i]+" is null or disabled!");
				continue;
			}
			if(use == null)
			{
				use = moveset[i];
				damage = calculateDamage(attacker, moveset[i],defender);
				continue;
			}
			Point moveDamage;
			if(moveset[i].name.toLowerCase().startsWith("counter") || moveset[i].name.toLowerCase().startsWith("mirror coat"))
				moveDamage = moveset[i].getProjectedDamage(defender);
			else
				moveDamage = calculateDamage(attacker, moveset[i],defender);
			if(moveDamage.y > damage.y)
			{
				damage=moveDamage;
				use = moveset[i];
			}
		}
		if(use == null)
		{
			use = new Move("Struggle", attacker);
		}
		return use;
	}
	
	
	public static int turnsToKill(int health, int damage)
	{
		if(damage == 0)
			return Integer.MAX_VALUE;
		return (int)Math.floor(health / damage);
	}
}