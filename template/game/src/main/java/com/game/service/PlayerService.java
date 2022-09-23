package com.game.service;

import com.game.controller.PlayerOrder;
import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Autowired
    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

                                            /**  Список всех игроков **/
    public List<Player> findAllPlayers(String name, String title, Race race, Profession profession, Long after, Long before, Boolean banned,
                                       Integer minExperience, Integer maxExperience, Integer minLevel, Integer maxLevel, PlayerOrder order,
                                       Integer pageNumber, Integer pageSize) {

        List<Player> players = filter(playerRepository.findAll(), name, title, race, profession, after,
                before, banned, minExperience, maxExperience, minLevel, maxLevel);

                                            /** Сортировка игроков  **/
        if (order.equals(PlayerOrder.ID)) players.sort(Comparator.comparing(Player::getId));
        if (order.equals(PlayerOrder.NAME)) players.sort(Comparator.comparing(Player::getName));
        if (order.equals(PlayerOrder.EXPERIENCE)) players.sort(Comparator.comparing(Player::getExperience));
        if (order.equals(PlayerOrder.BIRTHDAY)) players.sort(Comparator.comparing(Player::getBirthday));

                                               /** Пагинация  **/
        int endIndex = Math.min(((pageNumber + 1) * pageSize), (players.size()));
        int startIndex = pageNumber * pageSize;


        return players.subList(startIndex, endIndex);
    }

                                              /** Фильтрация игроков **/
    private List<Player> filter(List<Player> unfiltered, String name, String title, Race race, Profession profession,
                                Long after, Long before, Boolean banned, Integer minExperience,
                                Integer maxExperience, Integer minLevel, Integer maxLevel) {

        List<Player> filterPlayer = new ArrayList<>();

        unfiltered.forEach(player -> {
                    if ((name != null) && !(player.getName().toLowerCase().contains(name.toLowerCase()))) return;
                    if ((title != null) && !(player.getTitle().toLowerCase().contains(title.toLowerCase()))) return;
                    if ((race != null) && !(player.getRace() == race)) return;
                    if ((profession != null) && !(player.getProfession() == profession)) return;
                    if ((after != null) && (player.getBirthday().before(new Date(after)))) return;
                    if ((before != null) && (player.getBirthday().after(new Date(before)))) return;
                    if ((banned != null) && !(player.isBanned() == banned)) return;
                    if ((minExperience != null) && (player.getExperience().compareTo(minExperience) <= 0)) return;
                    if ((maxExperience != null) && (player.getExperience().compareTo(maxExperience) >= 0)) return;
                    if ((minLevel != null) && (player.getLevel().compareTo(minLevel) <= 0)) return;
                    if ((maxLevel != null) && (player.getLevel().compareTo(maxLevel) >= 0)) return;
                    filterPlayer.add(player);
                }
        );
        return filterPlayer;
    }

    public Integer getCount(String name, String title, Race race, Profession profession, Long after, Long before, Boolean banned,
                            Integer minExperience, Integer maxExperience, Integer minLevel, Integer maxLevel) {

        List<Player> players = filter(playerRepository.findAll(), name, title, race, profession, after,
                before, banned, minExperience, maxExperience, minLevel, maxLevel);

        return players.size();
    }

    public boolean isValidPlayer(Player player) {
        if (player.getName() == null || player.getName().length() > 12 || player.getName().isEmpty()) return false;
        if (player.getTitle() == null || player.getTitle().length() > 30) return false;
        if (player.getRace() == null) return false;
        if (player.getProfession() == null) return false;
        if (player.getBirthday() == null || 0 > player.getBirthday().getTime() ||
                player.getBirthday().after(new Date(1100, 0, 1)) ||
                player.getBirthday().before(new Date(100, 0, 1))) return false;
        if (player.isBanned() == null) player.setBanned(false);
        if (player.getExperience() == null || player.getExperience() > 10_000_000 || player.getExperience() < 0)
            return false;
        return true;

    }

    @Transactional
    public Player createPlayer(Player player) {
        player.setLevel(getLevel(player.getExperience()));
        player.setUntilNextLevel(getUntilNextLevel(player.getLevel(), player.getExperience()));
        return playerRepository.save(player);
    }

    private Integer getLevel(Integer ex) {
        ex = ex == null ? 0 : ex;
        return (int) ((Math.sqrt(2500.0 + 200.0 * ex) - 50) / 100);
    }

    private Integer getUntilNextLevel(Integer lvl, Integer ex) {
        ex = ex == null ? 0 : ex;
        return (50 * (lvl + 1) * (lvl + 2) - ex);
    }

    public boolean isValidId(Long id) {
        return id > 0;
    }

    public Player getPlayer(Long id) {
        return playerRepository.findById(id).orElse(null);

    }

    @Transactional
    public Player updatePlayer(Player player, Player updatedPlayer) {
        if (updatedPlayer.getName() != null) {
            if (updatedPlayer.getName().length() <= 12)
                player.setName(updatedPlayer.getName());
            else throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (updatedPlayer.getTitle() != null) {
            if (updatedPlayer.getTitle().length() <= 30)
                player.setTitle(updatedPlayer.getTitle());
            else throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (updatedPlayer.getRace() != null) player.setRace(updatedPlayer.getRace());
        if (updatedPlayer.getProfession() != null) player.setProfession(updatedPlayer.getProfession());
        if (updatedPlayer.getBirthday() != null) {
            if (0 < updatedPlayer.getBirthday().getTime() &&
                    updatedPlayer.getBirthday().before(new Date(1100, 0, 1)) &&
                    updatedPlayer.getBirthday().after(new Date(100, 0, 1)))
                player.setBirthday(updatedPlayer.getBirthday());
            else throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (updatedPlayer.isBanned() != null) player.setBanned(updatedPlayer.isBanned());
        if (updatedPlayer.getExperience() != null) {
            if (updatedPlayer.getExperience() > 0 && updatedPlayer.getExperience() < 10_000_000) {
                player.setExperience(updatedPlayer.getExperience());
                player.setLevel(getLevel(player.getExperience()));
                player.setUntilNextLevel(getUntilNextLevel(player.getLevel(), player.getExperience()));
            } else throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        }
        return playerRepository.save(player);
    }

    @Transactional
    public void deletePlayer(Long id) {
        playerRepository.deleteById(id);
    }

}
