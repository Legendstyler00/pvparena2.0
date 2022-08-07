## PVP Arena Placeholders

Since 2.0, PVPArena supports **placeholderAPI** and makes possible to display arena information with placeholders.
Here is the list of currently implemented placeholders.

> **🚩 Tip:** For all placeholders, you can replace arena name by "**cur**" to get current arena placeholders.

<br>

### Get current number of players / arena capacity
```
%pvpa_<arena>_capacity%
%pvpa_<arena>_capacity_team_<team>%
```
**\<arena\>**: name of the arena (or `cur`)  
**\<team\>**: the team name

**Note:** If maximums are set (for arena or teams) in your config file, the placeholder displays a ratio (like 1 / 15),
however it just prints the number of players in the team or in the arena.

<br>

### Get current top scores
```
%pvpa_<arena>_topscore_team_<nb>%         # Returns the team name matching to this ranking entry
%pvpa_<arena>_topscore_player_<nb>%       # Returns the player name matching to this ranking entry (FFA goals only)
%pvpa_<arena>_topscore_value_<nb>%        # Returns the score matching to this ranking entry
```
**\<arena\>**: name of the arena (or `cur`)  
**\<nb\>**: the rank from **0** to the max number of teams/players

For instance, if the arena is named CTF and  has 3 teams, you can make a quick score display like this:  
`%pvpa_CTF_topscore_team_0% %pvpa_CTF_topscore_value_0%`  
`%pvpa_CTF_topscore_team_1% %pvpa_CTF_topscore_value_1%`  
`%pvpa_CTF_topscore_team_2% %pvpa_CTF_topscore_value_2%`

<br>

### Get arena team of the current player
```
%pvpa_<arena>_team%
```
**\<arena\>**: name of the arena (or `cur`)

<br>

### Color a team name
```
%pvpa_<arena>_tcolor_{other_placeholder_without_percents}%
```
E.g. `%pvpa_Versus_tcolor_{pvpa_Versus_team}%`

<br>

### Color a player name
```
%pvpa_<arena>_pcolor_{other_placeholder_without_percents}%
```
E.g. `%pvpa_Versus_tcolor_{player_name}%`

<br>

### Get statistics / make a leaderboard

You can prints arena statistics to make a leaderboard. This placeholders make possible to print top 10 of each statistic
of each arena.
```
%pvpa_<arena>_stats_<stat>_player_<nb>%
%pvpa_<arena>_stats_<stat>_score_<nb>%
```
**\<arena\>**: name of the arena  
**\<stat\>**: name of the statistic (can be found [here](commands/stats.md#details))  
**\<nb\>**: line number between **0** and **9**  
<br>

Example: to make a top 3 of winners for the arena "Bastion" looking like this
> 17 Warrior55  
> 14 Xx_Duck_xX  
> 8 FluffyMike 

Use the following placeholders:  
`%pvpa_Bastion_stats_WINS_score_0% %pvpa_Bastion_stats_WINS_player_0%`  
`%pvpa_Bastion_stats_WINS_score_1% %pvpa_Bastion_stats_WINS_player_1%`  
`%pvpa_Bastion_stats_WINS_score_2% %pvpa_Bastion_stats_WINS_player_2%`
