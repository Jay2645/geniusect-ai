/*
 * A class representing a team.
 * @author TeamForretress
 */

package geniusect;

public class Team {
	public Team(int id)
	{
		teamID = id;
	}
	public Team(String input, int id)
	{
		teamID = id;
		//TODO: Mark end of one importable and beginning of another.
		String[] importable = input.split("\n\n", 6);
		for(int i = 0; i < importable.length; i++)
		{
			Spread spread = new Spread(Pokemon.loadFromText(importable[i], this,i), false);
			//TODO: Lookup GA data using specified Spread.
			addPokemon(spread);
		}
	}
	
	public Pokemon[] team = new Pokemon[6];
	public int teamID = -1;
	public boolean hasInitialized = false;
	
	public Pokemon addPokemon(String s)
	{
		for(int i = 0; i < team.length; i++)
		{
			if(team[i] == null)
				break;
			if(team[i].name.equals(s))
				return team[i];
		}
		Pokemon p = new Pokemon();
		p.name = s;
		return addPokemon(p);
	}
	public Pokemon addPokemon(Pokemon p)
	{
		//Adds Pokemon without marking them as active.
		//(Used if you know in advance what Pokemon a team has.)
		for(int i = 0; i < team.length; i++)
		{
			if(team[i] == null)
			{
				//System.err.println("Adding "+p.name+" to team ID "+teamID);
				if(p.team == null)
					p = new Pokemon(p.name,this,i);
				else p.id = i;
				team[i] = p;
				break;
			}
			if(team[i].name.equals(p.name))
				break;
		}
		hasInitialized = true;
		return p;
	}
}
