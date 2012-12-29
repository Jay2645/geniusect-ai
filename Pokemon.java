
/*
 * A class representing a Pokemon.
 * Keeps track of type, name, resistances, etc.
 * Also fills itself out with data on an unknown Pokemon as the match goes on.
 * @author TeamForretress
 */

package geniusect;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pokemon {
	public static Pokemon[] active = new Pokemon[2]; //Will need to adjust for double battles.
	public String name;
	public Item item;
	public Team team;
	public int level = 100;
	public Type[] types = {Type.None, Type.None};
	public Ability ability;
	public Nature nature = Nature.Hardy;
	public String tier;
	
	public ArrayList<Type> quadrupleEffective = new ArrayList<Type>();
	public ArrayList<Type> superEffective = new ArrayList<Type>();
	public ArrayList<Type> resistances = new ArrayList<Type>();
	public ArrayList<Type> doubleResistances = new ArrayList<Type>();
	public ArrayList<Type> immunities = new ArrayList<Type>();
	
	public Move[] moveset = {new Move("Struggle", this),new Move("Struggle", this), new Move("Struggle", this), new Move("Struggle", this)};
	
	public int[] base = {0,0,0,0,0,0};
	public int[] ivs = {31,31,31,31,31,31};
	public int[] evs = {0,0,0,0,0,0};
	public int[] stats = new int[6]; //Stats before boosts.
	protected int[] boosts = {0,0,0,0,0,0};
	protected int[] boostedStats = new int[6]; // Stats after boosts.
	public int fullHP; //How big of an HP stat we have at full HP.
	public int hpPercent = 100; //Our current HP percent.
	public int evsLeft = 510; //Legality check for our EV calculations.
	
	public boolean lead = false; //Is this the lead?
	protected boolean alive = true; //Is this Pokemon alive?
	protected boolean canMove = true; //Can we move?
	protected Status status = Status.None; //What permanent status do we have (i.e. Poison, Burn, etc.)?
	protected ArrayList<VolatileStatus> effects = new ArrayList<VolatileStatus>(); //What temporary status do we have (i.e. Confused, Taunt, etc.)?
	protected Pokemon enemy;
	
	public Pokemon() {}
	
	public Pokemon(Pokemon clone)
	{
		name = clone.name;
		item = clone.item;
		team = clone.team;
		level = clone.level;
		types = clone.types;
		ability = clone.ability;
		nature = clone.nature;
		tier = clone.tier;
		
		quadrupleEffective = clone.quadrupleEffective;
		superEffective = clone.superEffective;
		resistances = clone.resistances;
		doubleResistances = clone.doubleResistances;
		immunities = clone.immunities;
		
		moveset[0] = new Move(clone.moveset[0]);
		moveset[1] = new Move(clone.moveset[1]);
		moveset[2] = new Move(clone.moveset[2]);
		moveset[3] = new Move(clone.moveset[3]);
		
		base = clone.base;
		ivs = clone.ivs;
		evs = clone.evs;
		stats = clone.stats;
		boosts = clone.boosts;
		boostedStats = clone.boostedStats;
		fullHP = clone.fullHP;
		hpPercent = clone.hpPercent;
		evsLeft = clone.evsLeft;
		
		lead = clone.lead;
		alive = clone.alive;
		canMove = clone.canMove;
		status = clone.status;
		effects = clone.effects;
		enemy = clone.enemy;
	}
	
	public Pokemon(String n, Team t)
	{
		name = n;
		team = t;
		query();
	}
	
	public void query()
	{
		SQLHandler.queryPokemon(this);
		stats = Pokequations.calculateStat(this);
		boostedStats = stats;
		fullHP = boostedStats[Stat.HP.toInt()];
		if(active[0] == null && active[1] == null || active[0] == null && active[1].team != this.team || active[0].team != this.team && active[1] == null)
		{
			onSendOut();
		}
	}
	
	public void onSendOut()
	{
		//Called when this Pokemon enters the battle.
		boolean entered = false;
		for(int i = 0; i < active.length; i++)
		{
			if(active[i] == null)
			{
				active[i] = this;
				entered = true;
			}
		}
		if(!entered)
			GeniusectAI.print("Could not mark "+name+" as the active Pokemon!");
		if(team == GeniusectAI.us && name.toLowerCase().startsWith("wobbuffet") || name.toLowerCase().startsWith("wynaut"))
		{
			GeniusectAI.setGeneric();
		}
		status.resetActive();
	}
	
	public void onWithdraw()
	{
		//Called when this Pokemon withdraws from the battle.
		for(int i = 0; i < active.length; i++)
		{
			if(active[i] == this)
				active[i] = null;
		}
		resetBoosts();
		effects.clear();
		if(team == GeniusectAI.us && name.toLowerCase().startsWith("wobbuffet") || team == GeniusectAI.us && name.toLowerCase().startsWith("wynaut"))
		{
			GeniusectAI.setMiniMax();
		}
	}
	
	public void onDie()
	{
		//Called when the Pokemon dies.
		onWithdraw();
		alive = false;
	}
	
	public int onNewTurn(Attack a)
	{
		//Called when predicting future events.
		//Whereas the method below takes the moves that THIS Pokemon did, this method takes a move the OTHER Pokemon did.
		//It returns the amount of damage done in this turn.
		int preHP = hpPercent;
		damage(a);
		damage((int)Math.round(status.newTurn()));
		return preHP - hpPercent;
	}
	
	public void onNewTurn(String n, int damageDone, boolean crit)
	{
		//Called when this Pokemon uses a move.
		//Keeps track of what moves THIS Pokemon has done (if unknown) and what damage they did to the enemy.
		for(int i = 0; i < moveset.length; i++)
		{
			if(moveset[i].name.toLowerCase().startsWith("Struggle") && !n.toLowerCase().startsWith("Struggle"))
			{
				moveset[i] = new Move(n,this);
				moveset[i].onMoveUsed(enemy, damageDone, crit);
				break;
			}
			else if(moveset[i].name.toLowerCase().startsWith(n))
			{
				moveset[i].onMoveUsed(enemy, damageDone, crit);
				break;
			}
		}
		status.newTurn();
	}
	
	public int restoreHP(int restorePercent)
	{
		int restoreAmount = restorePercent + hpPercent;
		if(restoreAmount > 100)
		{
			int difference = restoreAmount - 100;
			restoreAmount -= difference;
		}
		hpPercent = restoreAmount;
		return restoreAmount;
	}
	
	public boolean damage(int damagePercent)
	{
		hpPercent -= damagePercent;
		if(hpPercent < 0)
			hpPercent = 0;
		alive = hpPercent > 0;
		return alive;
	}
	
	public boolean damage(Attack attack)
	{
		return damage(attack.move.getProjectedPercent(this).y);
	}
	
	/*
	 * 
	 * Query methods below here.
	 * 
	 */
	
	public boolean isAlive()
	{
		return alive;
	}
	
	public boolean canMove()
	{
		return canMove;
	}
	
	public void canMove(boolean can)
	{
		canMove = can;
	}
	
	public boolean isFasterThan(Pokemon compare)
	{
		return compare.boostedStats[Stat.Spe.toInt()] < boostedStats[Stat.Spe.toInt()];
	}
	
	public boolean isFasterThan(int speed)
	{
		//Useful for making self faster than "Magic Numbers" when building a team.
		return speed < boostedStats[Stat.Spe.toInt()];
	}
	
	public int[] boostedStats()
	{
		return boostedStats;
	}
	
	public int boostedStat(Stat stat)
	{
		return boostedStats[stat.toInt()];
	}
	
	public void changeEnemy(Pokemon e)
	{
		enemy = e;
	}
	
	public int getHealth()
	{
		return hpPercent;
	}
	
	/*
	 * 
	 * Logic methods below here.
	 * 
	 */
	
	public void adjustEVs(int[] newEVs)
	{
		//Adjust our EV spread.
		evsLeft = 510;
		for(int i = 0; i < evs.length; i++)
		{
			evs[i] = 0;
			adjustEVs(i,newEVs[i]);
		}
	}
	
	public void adjustEVs(int index, int newEV)
	{
		if(Stat.fromInt(index) == Stat.HP)
		{
			stats[Stat.HP.toInt()] = fullHP;
		}
		evsLeft += evs[index];
		int check = evsLeft - newEV;
		if(check < 0)
		{
			GeniusectAI.print("EV spread is invalid!");
			return;
		}
		evs[index] = newEV;
		Pokequations.calculateStat(Stat.fromInt(index),this);
		if(Stat.fromInt(index) == Stat.HP)
		{
			fullHP = stats[Stat.HP.toInt()];
			percentToHP();
		}
	}
	
	public void inflictStatus(Status s)
	{
		if(status == Status.None || s == Status.Rest || s == Status.None)
		{
			status = s;
		}
	}
	
	public void inflictStatus(VolatileStatus s)
	{
		s.inflict(this); //Will change our effects ArrayList if possible.
	}
	
	public void giveBoosts(Stat boost, int count)
	{
		boosts[boost.toInt()] += count;
		if(boosts[boost.toInt()] > 6)
			boosts[boost.toInt()] = 6;
		else if(boosts[boost.toInt()] < -6)
			boosts[boost.toInt()] = -6;
		boostedStats[boost.toInt()] = Pokequations.statBoost(boosts[boost.toInt()],stats[boost.toInt()]);
	}
	
	public int hpToPercent()
	{
		hpPercent = (int)Math.round(stats[Stat.HP.toInt()] / fullHP);
		return hpPercent;
	}
	
	public int hpToPercent(int hp)
	{
		return (int)Math.round((hp / fullHP) * 100);
	}
	
	public int percentToHP()
	{
		stats[Stat.HP.toInt()] = (int)Math.round(fullHP * (hpPercent / 100));
		return stats[Stat.HP.toInt()];
	}
	
	public void resetBoosts()
	{
		for(int i = 0; i < 6; i++)
		{
			boosts[i] = 0;
		}
		boostedStats = stats;
	}
	
	
	/*
	 * 
	 * Static methods below here.
	 * 
	 */
	
	public static Pokemon loadFromText(String importable, Team t)
	{
		if(importable.isEmpty())
			return null;
		System.out.println("Loading importable: " + importable);
		Pokemon found = null;
		Boolean evsFound = false;
		
		
		Pattern p = Pattern.compile("(.+) @ (.+)\\s+Trait: (.+)\n.+\\s+(.+)Nature\\s+", Pattern.MULTILINE);
		Matcher m = p.matcher(importable);
		if(m.find()) // only need 1 find for this
		{
			found = new Pokemon();
			found.name = importable.substring(m.start(1), m.end(1));
			found.item = new Item(importable.substring(m.start(2), m.end(2)));
			found.ability = new Ability(importable.substring(m.start(3), m.end(3)));
			found.nature = Nature.fromString(importable.substring(m.start(4), m.end(4)));
							
			System.out.println("name: " + found.name);
			System.out.println("item: " + found.item.name);
			System.out.println("trait: " + found.ability.name);
			System.out.println("nature: " + found.nature.toString());
		}
		String[] evP = {"(\\d+) HP","(\\d+) Atk","(\\d+) Def","(\\d+) SAtk","(\\d+) SDef","(\\d+) Spd"};
		int[] evDist = new int[6];
		for (int i = 0; i < 6; ++i)
		{
			evDist[i] = 0;
			Matcher m2 = Pattern.compile(evP[i], Pattern.MULTILINE).matcher(importable);
			if (m2.find())
			{
				evDist[i] = Integer.parseInt(importable.substring(m2.start(1), m2.end(1)));
				evsFound = true;
			}
		}
		System.out.println("Stats: ");
		System.out.printf("%d hp, %d atk, %d def, %d spa, %d spd, %d spe\n", evDist[0], evDist[1], evDist[2], evDist[3], evDist[4], evDist[5]);
					
		Pattern moveP = Pattern.compile("- (.+)\\n*", Pattern.MULTILINE);
		m = moveP.matcher(importable);
		String moves[] = new String[4];
		System.out.println("moves: ");
		int i = 0;
		while (m.find())
		{
			moves[i] = importable.substring(m.start(1), m.end(1));
			found.moveset[i] = new Move(moves[i],found);
			i++;
		}
		
		if(found == null){
			System.out.format("No Pokemon was found.%n");
			return null;
		}
		found.team = t;
		if(evsFound)
		{
			found.evs = evDist;
		}
		else
		{
			System.out.format("No EVs were found.%n");
		}
		found.query();
		return found;
	}
}