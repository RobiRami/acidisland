package com.wasteofplastic.acidisland;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.evilmidget38.NameFetcher;

/**
 * Tracks the following info on the player
 */
public class Players {
    private AcidIsland plugin;
    private YamlConfiguration playerInfo;
    private HashMap<String, Boolean> challengeList;
    private boolean hasIsland;
    private boolean inTeam;
    private String homeLocation;
    private int islandLevel;
    private String islandLocation;
    private List<UUID> members;
    private String teamIslandLocation;
    private UUID teamLeader;
    private UUID uuid;
    private String playerName;

    /**
     * @param uuid
     *            Constructor - initializes the state variables
     * 
     */
    public Players(final AcidIsland acidIsland, final UUID uuid) {
	this.plugin = acidIsland;
	this.uuid = uuid;
	this.members = new ArrayList<UUID>();
	this.hasIsland = false;
	this.islandLocation = null;
	this.homeLocation = null;
	this.inTeam = false;
	this.teamLeader = null;
	this.teamIslandLocation = null;
	this.challengeList = new HashMap<String, Boolean>();
	this.islandLevel = 0;
	this.playerName = "";
	load(uuid);
    }

    /**
     * Loads a player from file system and if they do not exist, then it is created
     * @param uuid
     */
    public void load(UUID uuid) {
	playerInfo = AcidIsland.loadYamlFile("players/" + uuid.toString() + ".yml");
	// Load in from YAML file
	this.playerName = playerInfo.getString("playerName", "");
	if (playerName.isEmpty()) {
	    // Some issue with the name - try and retrieve from Mojang
		NameFetcher fetcher = new NameFetcher(Arrays.asList(uuid));
		Map<UUID, String> response = null;
		try {
		response = fetcher.call();
		} catch (Exception e) {
		plugin.getLogger().warning("Exception while running NameFetcher");
		e.printStackTrace();
		}
		if (!response.isEmpty()) {
		    playerName = response.get(uuid);
		}
	}
	plugin.getLogger().info("Loading player..." + playerName);
	this.hasIsland = playerInfo.getBoolean("hasIsland", false);
	this.islandLocation = playerInfo.getString("islandLocation", "");
	this.homeLocation = playerInfo.getString("homeLocation", "");
	this.inTeam = playerInfo.getBoolean("hasTeam", false);
	final String teamLeaderString = playerInfo.getString("teamLeader", "");
	if (!teamLeaderString.isEmpty()) {
	    this.teamLeader = UUID.fromString(teamLeaderString);
	} else {
	    this.teamLeader = null;
	}
	this.teamIslandLocation = playerInfo.getString("teamIslandLocation", "");
	this.islandLevel = playerInfo.getInt("islandLevel", 0);
	List<String> temp = playerInfo.getStringList("members");
	for (String s: temp) {
	    this.members.add(UUID.fromString(s));
	}
	// Challenges
	// Run through all challenges available
	for (String challenge : Settings.challengeList) {
	    // If they are in the list, then use the value, otherwise use false
	    challengeList.put(challenge, playerInfo.getBoolean("challenges.status." + challenge, false));
	}
    }

    /**
     * Saves the player info to the file system
     */
    public void save() {
	plugin.getLogger().info("Saving player..." + playerName);
	// Save the variables
	playerInfo.set("playerName", playerName);
	playerInfo.set("hasIsland", hasIsland);
	playerInfo.set("islandLocation", islandLocation);
	playerInfo.set("homeLocation", homeLocation);
	playerInfo.set("hasTeam", inTeam);
	if (teamLeader == null) {
	    playerInfo.set("teamLeader","");
	} else {
	    playerInfo.set("teamLeader", teamLeader.toString());
	}
	playerInfo.set("teamIslandLocation", teamIslandLocation);
	playerInfo.set("islandLevel", islandLevel);
	// Serialize UUIDs
	List<String> temp = new ArrayList<String>();
	for (UUID m: members) {
	    temp.add(m.toString());
	}
	playerInfo.set("members", temp);
	// Get the challenges
	for (String challenge : challengeList.keySet()) {
	    playerInfo.set("challenges.status." + challenge, challengeList.get(challenge));
	}
	AcidIsland.saveYamlFile(playerInfo, "players/" + uuid.toString() + ".yml");
    }

    /**
     * @param member
     *            Adds a member to the the player's list
     */
    public void addTeamMember(final UUID member) {
	members.add(member);
    }

    /**
     * A maintenance function. Rebuilds the challenge list for this player.
     * Should be used when the challenges change, e.g. config.yml changes.
     */
    public void updateChallengeList() {
	// If it does not exist, then make it
	if (challengeList == null) {
	    challengeList = new HashMap<String, Boolean>();
	}
	// Iterate through all the challenges in the config.yml and if they are
	// not in the list the add them as yet to be done
	final Iterator<?> itr = Settings.challengeList.iterator();
	while (itr.hasNext()) {
	    final String current = (String) itr.next();
	    if (!challengeList.containsKey(current.toLowerCase())) {
		challengeList.put(current.toLowerCase(), Boolean.valueOf(false));
	    }
	}
	// If the challenge list is bigger than the number of challenges in the
	// config.yml (some were removed?)
	// then remove the old ones - the ones that are no longer in Settings
	if (challengeList.size() > Settings.challengeList.size()) {
	    final Object[] challengeArray = challengeList.keySet().toArray();
	    for (int i = 0; i < challengeArray.length; i++) {
		if (!Settings.challengeList.contains(challengeArray[i].toString())) {
		    challengeList.remove(challengeArray[i].toString());
		}
	    }
	}
    }

    /**
     * Checks if a challenge exists in the player's challenge list
     * 
     * @param challenge
     * @return true if challenge is listed in the player's challenge list,
     *         otherwise false
     */
    public boolean challengeExists(final String challenge) {
	if (challengeList.containsKey(challenge.toLowerCase())) {
	    return true;
	}
	// for (String s : challengeList.keySet()) {
	// AcidIsland.getInstance().getLogger().info("DEBUG: challenge list: " +
	// s);
	// }
	return false;
    }

    /**
     * Checks if a challenge is recorded as completed in the player's challenge
     * list or not
     * 
     * @param challenge
     * @return true if the challenge is listed as complete, false if not
     */
    public boolean checkChallenge(final String challenge) {
	if (challengeList.containsKey(challenge.toLowerCase())) {
	    //plugin.getLogger().info("DEBUG: " + challenge + ":" + challengeList.get(challenge.toLowerCase()).booleanValue() );
	    return challengeList.get(challenge.toLowerCase()).booleanValue();
	}
	return false;
    }

    /**
     * Records the challenge as being complete in the player's list If the
     * challenge is not listed in the player's challenge list already, then it
     * will not be recorded! TODO: Possible systemic bug here as a result
     * 
     * @param challenge
     */
    public void completeChallenge(final String challenge) {
	if (challengeList.containsKey(challenge)) {
	    challengeList.remove(challenge);
	    challengeList.put(challenge, Boolean.valueOf(true));
	}
    }

    public boolean hasIsland() {
	return hasIsland;
    }

    /**
     * 
     * @return boolean - true if player is in a team
     */
    public boolean inTeam() {
	if (members == null) {
	    members = new ArrayList<UUID>();
	}
	return inTeam;
    }

    public Location getHomeLocation() {
	// return homeLoc.getLocation();
	return getLocationString(homeLocation);
    }

    /**
     * @return The island level int. Note this function does not calculate the
     *         island level
     */
    public int getIslandLevel() {
	return islandLevel;
    }

    /**
     * @return the location of the player's island in Location form
     */
    public Location getIslandLocation() {
	return getLocationString(islandLocation);
    }

    /**
     * Converts a serialized location string to a Bukkit Location
     * 
     * @param s
     *            - a serialized Location
     * @return a new Location based on string or null if it cannot be parsed
     */
    private static Location getLocationString(final String s) {
	if (s == null || s.trim() == "") {
	    return null;
	}
	final String[] parts = s.split(":");
	if (parts.length == 4) {
	    final World w = Bukkit.getServer().getWorld(parts[0]);
	    final int x = Integer.parseInt(parts[1]);
	    final int y = Integer.parseInt(parts[2]);
	    final int z = Integer.parseInt(parts[3]);
	    return new Location(w, x, y, z);
	}
	return null;
    }

    public List<UUID> getMembers() {
	return members;
    }

    public Location getTeamIslandLocation() {
	// return teamIslandLoc.getLocation();
	return getLocationString(teamIslandLocation);
    }

    public UUID getTeamLeader() {
	return teamLeader;
    }

    public Player getPlayer() {
	return Bukkit.getPlayer(uuid);
    }

    public UUID getPlayerUUID() {
	return uuid;
    }
    
    public String getPlayerName() {
	return playerName;
    }
    
    public void setPlayerN(String playerName) {
	this.playerName = playerName;
    }
    
    /**
     * Converts a Bukkit location to a String
     * 
     * @param l
     *            a Bukkit Location
     * @return String of the floored block location of l or "" if l is null
     */

    private String getStringLocation(final Location l) {
	if (l == null) {
	    return "";
	}
	return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    /**
     * Removes member from player's member list
     * 
     * @param member
     */
    public void removeMember(final UUID member) {
	members.remove(member);
    }

    /**
     * Resets all the challenges for the player and rebuilds the challenge list
     */
    public void resetAllChallenges() {
	challengeList = null;
	updateChallengeList();
    }

    /**
     * Resets a specific challenge. Will not reset a challenge that does not
     * exist in the player's list TODO: Add a success or failure return
     * 
     * @param challenge
     */
    public void resetChallenge(final String challenge) {
	if (challengeList.containsKey(challenge)) {
	    challengeList.remove(challenge);
	    challengeList.put(challenge, Boolean.valueOf(false));
	}
    }

    public void setHasIsland(final boolean b) {
	hasIsland = b;
    }

    /**
     * Stores the home location of the player in a String format
     * 
     * @param l
     *            a Bukkit location
     */
    public void setHomeLocation(final Location l) {
	homeLocation = getStringLocation(l);
    }

    /**
     * Records the island's level. Does not calculate it
     * 
     * @param i
     */
    public void setIslandLevel(final int i) {
	islandLevel = i;
    }

    /**
     * Records the player's island location in a string form
     * 
     * @param l
     *            a Bukkit Location
     */
    public void setIslandLocation(final Location l) {
	islandLocation = getStringLocation(l);
    }

    /**
     * Records that a player is now in a team
     * 
     * @param leader
     *            - a String of the leader's name
     * @param l
     *            - the Bukkit location of the team's island (converted to a
     *            String in this function)
     */
    public void setJoinTeam(final UUID leader, final Location l) {
	inTeam = true;
	teamLeader = leader;
	teamIslandLocation = getStringLocation(l);
    }

    /**
     * Called when a player leaves a team Resets hasTeam, teamLeader,
     * islandLevel, teamIslandLocation and members array
     */
 
    public void setLeaveTeam() {
	inTeam = false;
	teamLeader = null;
	islandLevel = 0;
	teamIslandLocation = null;
	islandLocation = null;
	homeLocation = null;
	members = new ArrayList<UUID>();
    }

    /**
     * Sets the members array to the list newMembers
     * 
     * @param newMembers
     *            a String List
     */
    public void setMembers(final List<UUID> newMembers) {
	members = newMembers;
    }

    /**
     * @param l
     *            a Bukkit Location of the team island
     */
    public void setTeamIslandLocation(final Location l) {
	teamIslandLocation = getStringLocation(l);
    }

    /**
     * @param leader
     *            a String name of the team leader
     */
    public void setTeamLeader(final UUID leader) {
	teamLeader = leader;
    }

    /**
     * @param s
     *            a String name of the player
     */
    public void setPlayerUUID(final UUID s) {
	uuid = s;
    }

    public void setHL(String hl) {
	homeLocation = hl;
    }

}