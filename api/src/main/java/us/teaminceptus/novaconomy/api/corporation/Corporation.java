package us.teaminceptus.novaconomy.api.corporation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.market.StockHolder;
import us.teaminceptus.novaconomy.api.events.corporation.CorporationCreateEvent;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a Novaconomy Corporation
 */
public final class Corporation implements StockHolder {

    // Constants

    /**
     * The maximum length of a corporation name
     */
    public static final int MAX_NAME_LENGTH = 32;

    // Class

    private final UUID id;
    private final long creationDate;
    private final OfflinePlayer owner;

    private final File folder;

    private String name;
    private Material icon;
    private Location headquarters = null;

    private double experience = 0.0;

    private final List<Business> children = new ArrayList<>();
    private final Map<CorporationAchievement, Integer> achievements = new HashMap<>();

    {
        for (CorporationAchievement value : CorporationAchievement.values()) achievements.putIfAbsent(value, 0);
    }

    private Corporation(@NotNull UUID id, long creationDate, OfflinePlayer owner) {
        this.id = id;
        this.creationDate = creationDate;
        this.owner = owner;

        this.folder = new File(NovaConfig.getCorporationsFolder(), id.toString());
    }

    /**
     * Fetches the ID of this Corporation.
     * @return Corporation ID
     */
    @NotNull
    public UUID getUniqueId() { return id; }

    /**
     * Fetches the folder that this corporation's data is stored in.
     * @return Corporation Folder
     */
    @NotNull
    public File getFolder() { return folder; }

    /**
     * Fetches the date that this Corporation was created.
     * @return Corporation Creation Date
     */
    @NotNull
    public Date getCreationDate() {
        return new Date(creationDate);
    }

    /**
     * Fetches the owner of this Corporation.
     * @return Corporation Owner
     */
    @NotNull
    public OfflinePlayer getOwner() {
        return owner;
    }

    // Info

    /**
     * Fetches all of the Businesses this Corporation is responsible for.
     * @return Business Children
     */
    @NotNull
    public List<Business> getChildren() {
        return children;
    }

    /**
     * Adds a Business to this Corporation's children.
     * @param b Business to add
     * @throws IllegalArgumentException if the Business already has a parent corporation, or is null
     */
    public void addChild(@NotNull Business b) throws IllegalArgumentException {
        if (b == null) throw new IllegalArgumentException("Business cannot be null");
        if (b.getParentCorporation() != null) throw new IllegalArgumentException("Business already has a parent corporation");
        if (children.contains(b)) throw new IllegalArgumentException("Business is already a child of this corporation");

        children.add(b);
        saveCorporation();
    }

    /**
     * Removes a Business from this Corporation's children.
     * @param b Business to remove
     * @throws IllegalArgumentException if the Business is not a child of this Corporation, is null, or Business matches owner's business
     */
    public void removeChild(@NotNull Business b) throws IllegalArgumentException {
        if (b == null) throw new IllegalArgumentException("Business cannot be null");
        if (!b.getParentCorporation().equals(this)) throw new IllegalArgumentException("Business is not a child of this corporation");
        if (b.getOwner().equals(owner)) throw new IllegalArgumentException("Cannot remove a business owned by the corporation owner");

        children.remove(b);
        saveCorporation();
    }

    /**
     * Fetches the name of this Corporation.
     * @return Corporation Name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this Corporation.
     * @param name New Corporation Name
     * @throws IllegalArgumentException if name is too long according to {@link #MAX_NAME_LENGTH} or is null
     */
    public void setName(@NotNull String name) throws IllegalArgumentException {
        if (name == null) throw new IllegalArgumentException("Name cannot be null!");
        if (name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Corporation name cannot be longer than 32 characters.");

        this.name = name;
        saveCorporation();
    }

    /**
     * Fetches the amount of experience this Corporation has.
     * @return Corporation Experience
     */
    public double getExperience() {
        return experience;
    }

    /**
     * Sets the amount of experience this Corporation has.
     * @param experience New Corporation Experience
     * @throws IllegalArgumentException if experience is negative
     */
    public void setExperience(double experience) throws IllegalArgumentException {
        if (experience < 0) throw new IllegalArgumentException("Corporation experience cannot be negative!");
        this.experience = experience;
        saveCorporation();
    }

    /**
     * Adds experience to this Corporation.
     * @param add Experience to add
     * @throws IllegalArgumentException if result is negative
     */
    public void addExperience(double add) throws IllegalArgumentException {
        setExperience(experience + add);
    }

    /**
     * Removes experience from this Corporation.
     * @param remove Experience to remove
     * @throws IllegalArgumentException if result is negative
     */
    public void removeExperience(double remove) throws IllegalArgumentException {
        setExperience(experience - remove);
    }

    /**
     * Fetches this Corporation's Level.
     * @return Corporation Level
     */
    public int getLevel() {
        return toLevel(experience);
    }

    /**
     * Sets this corporation's level, setting the experience to the minimum required.
     * @param level New Corporation Level
     * @throws IllegalArgumentException if level is not postiive
     */
    public void setLevel(int level) throws IllegalArgumentException {
        if (level < 1) throw new IllegalArgumentException("Corporation level must be postiive!");
        setExperience(toExperience(level));
    }

    /**
     * Fetches this Corporation's Icon.
     * @return Corporation Icon
     */
    @NotNull
    public Material getIcon() {
        return icon;
    }

    /**
     * Sets this Corporation's Icon.
     * @param icon New Corporation Icon
     * @throws IllegalArgumentException if icon is null
     */
    public void setIcon(@NotNull Material icon) throws IllegalArgumentException {
        if (icon == null) throw new IllegalArgumentException("Corporation Icon cannot be null!");
        this.icon = icon;
        saveCorporation();
    }

    /**
     * Fetches the Location of this Corporation's Headquarters.
     * @return Corporation Headquarters
     */
    @Nullable
    public Location getHeadquarters() {
        return headquarters;
    }

    /**
     * Sets the Location of this Corporation's Headquarters.
     * @param headquarters New Corporation Headquarters
     */
    public void setHeadquarters(@Nullable Location headquarters) {
        this.headquarters = headquarters;
        saveCorporation();
    }

    /**
     * Fetches an immutable version of all of the Corporation's Achievements to their level.
     * @return Corporation Achievements
     */
    @NotNull
    public Map<CorporationAchievement, Integer> getAchievements() {
        return ImmutableMap.copyOf(achievements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Corporation that = (Corporation) o;
        return creationDate == that.creationDate && id.equals(that.id) && owner.equals(that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, creationDate, owner.getUniqueId());
    }

    @Override
    public String toString() {
        return "Corporation{" +
                "id=" + id +
                ", creationDate=" + creationDate +
                ", owner=" + owner +
                ", name='" + name + '\'' +
                '}';
    }

    // Static Methods

    private static final Set<Corporation> CORPORATION_CACHE = new HashSet<>();

    /**
     * Fetches an immutable set of all of the corporations that exist.
     * @return All Corporations
     */
    @NotNull
    public static Set<Corporation> getCorporations() {
        if (!CORPORATION_CACHE.isEmpty()) return ImmutableSet.copyOf(CORPORATION_CACHE);
        Set<Corporation> corporations = new HashSet<>();

        for (File folder : NovaConfig.getCorporationsFolder().listFiles()) {
            if (folder == null) continue;
            if (!folder.isDirectory()) continue;

            Corporation c;

            try {
                c = readCorporation(folder.getAbsoluteFile());
            } catch (OptionalDataException e) {
                NovaConfig.print(e);
                continue;
            } catch (IOException | ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }

            corporations.add(c);
        }

        CORPORATION_CACHE.addAll(corporations);
        return ImmutableSet.copyOf(CORPORATION_CACHE);
    }

    /**
     * Fetches a Corporation by its name.
     * @param name Corporation Name
     * @return Corporation found, or null if not found
     */
    public static Corporation byName(@Nullable String name) {
        if (name == null) return null;
        return getCorporations().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Fetches a Corporation by its ID.
     * @param id Corporation ID
     * @return Corporation found, or null if not found
     */
    public static Corporation byId(@Nullable UUID id) {
        if (id == null) return null;
        return getCorporations().stream()
                .filter(c -> c.getUniqueId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Fetches a Corporation by its owner.
     * @param owner Corporation Owner
     * @return Corporation found, or null if not found
     */
    public static Corporation byOwner(@Nullable OfflinePlayer owner) {
        if (owner == null) return null;
        return getCorporations().stream()
                .filter(c -> c.getOwner().equals(owner))
                .findFirst()
                .orElse(null);
    }

    /**
     * Whether a Corporation exists with the given name.
     * @param name Corporation Name
     * @return true if exists, false otherwise
     */
    public static boolean exists(@Nullable String name) {
        if (name == null) return false;
        return getCorporations().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name));
    }

    /**
     * Whether a Corporation exists with the given ID.
     * @param id Corporation ID
     * @return true if exists, false otherwise
     */
    public static boolean exists(@Nullable UUID id) {
        if (id == null) return false;
        return getCorporations().stream()
                .anyMatch(c -> c.getUniqueId().equals(id));
    }

    /**
     * Whether a Corporation exists with the given owner.
     * @param owner Corporation Owner
     * @return true if exists, false otherwise
     */
    public static boolean exists(@Nullable OfflinePlayer owner) {
        if (owner == null) return false;
        return getCorporations().stream()
                .anyMatch(c -> c.getOwner().equals(owner));
    }

    /**
     * Deletes a Corporation.
     * @param c Corporation to delete
     */
    public static void removeCorporation(@NotNull Corporation c) {
        if (c == null) throw new IllegalArgumentException("Corporation cannot be null!");
        CORPORATION_CACHE.clear();

        for (File f : c.folder.listFiles()) {
            if (f == null) continue;
            f.delete();
        }

        c.folder.delete();
    }

    /**
     * Converts the experience of a Corporation to a level.
     * @param level Level to convert to
     * @return Minimum Experience required for the specified level
     * @throws IllegalArgumentException if level is not positive
     */
    public static double toExperience(int level) throws IllegalArgumentException {
        if (level < 1) throw new IllegalArgumentException("Level must be positive!");
        if (level == 1) return 0;

        double level0 = level - 1;
        double num = Math.floor(Math.pow(2, level0 - 1) * 10000 * level0);
        double rem = num % 1000;

        return rem >= 1000 / 2D ? num - rem + 1000 : num - rem;
    }

    /**
     * Converts the experience of a Corporation to a level.
     * @param experience Experience to convert to
     * @return Level conversion at the specified experience
     */
    public static int toLevel(double experience) {
        int level = 1;
        while (toExperience(level) <= experience) level++;
        return level;
    }

    // Builder

    /**
     * Creates a new Corporation Builder.
     * @return New Corporation Builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class used to create Corporations
     */
    public static final class Builder {

        String name;
        OfflinePlayer owner;
        Material icon = Material.STONE;
        Location headquarters;

        Builder() {}

        /**
         * Sets the name of the Corporation.
         * @param name Corporation Name
         * @return this class, for chaining
         */
        public Builder setName(@NotNull String name) {
            if (name == null) throw new IllegalArgumentException("Corporation Name cannot be null!");
            this.name = name;
            return this;
        }

        /**
         * Sets the owner of the Corporation.
         * @param owner Corporation Owner
         * @return this class, for chaining
         */
        public Builder setOwner(@NotNull OfflinePlayer owner) {
            if (owner == null) throw new IllegalArgumentException("Corporation Owner cannot be null!");
            this.owner = owner;
            return this;
        }

        /**
         * Sets the icon of the Corporation.
         * @param icon Corporation Icon
         * @return this class, for chaining
         */
        public Builder setIcon(@NotNull Material icon) {
            if (icon == null) throw new IllegalArgumentException("Corporation Icon cannot be null!");
            this.icon = icon;
            return this;
        }

        /**
         * Sets the headquarters of the Corporation.
         * @param headquarters Corporation Headquarters
         * @return this class, for chaining
         */
        public Builder setHeadquarters(@Nullable Location headquarters) {
            this.headquarters = headquarters;
            return this;
        }

        /**
         * Builds the Corporation.
         * @return New Corporation
         * @throws IllegalStateException if one or more arguments is null
         * @throws UnsupportedOperationException if corporation with name already exists
         */
        @NotNull
        public Corporation build() throws IllegalStateException, UnsupportedOperationException {
            if (name == null) throw new IllegalStateException("Corporation Name cannot be null!");
            if (owner == null) throw new IllegalStateException("Corporation Owner cannot be null!");
            if (icon == null) throw new IllegalStateException("Corporation Icon cannot be null!");
            if (Corporation.exists(name)) throw new UnsupportedOperationException("Corporation with name already exists!");

            CORPORATION_CACHE.clear();
            UUID uid = UUID.randomUUID();

            Corporation c = new Corporation(uid, System.currentTimeMillis(), owner);
            c.name = name;
            c.icon = icon;
            c.headquarters = headquarters;

            if (Business.exists(owner)) c.children.add(Business.getByOwner(owner));

            c.saveCorporation();

            CorporationCreateEvent event = new CorporationCreateEvent(c);
            Bukkit.getPluginManager().callEvent(event);
            return c;
        }
    }

    // Reading & Writing

    /**
     * <p>Saves this Corporation to its Corporation file.</p>
     * <p>This method is called automatically.</p>
     */
    public void saveCorporation() {
        try {
            writeCorporation();
        } catch (IOException e) {
            NovaConfig.print(e);
        }
    }

    private void writeCorporation() throws IOException {
        if (!folder.exists()) folder.mkdir();

        File info = new File(folder, "info.dat");
        if (!info.exists()) info.createNewFile();

        ObjectOutputStream infoOs = new ObjectOutputStream(Files.newOutputStream(info.toPath()));
        infoOs.writeObject(this.id);
        infoOs.writeLong(this.creationDate);
        infoOs.writeObject(this.owner.getUniqueId());
        infoOs.close();

        File dataF = new File(folder, "data.yml");
        if (!dataF.exists()) dataF.createNewFile();

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataF);
        data.set("name", this.name);
        data.set("experience", this.experience);
        data.set("icon", this.icon.name());
        data.set("headquarters", this.headquarters);
        data.save(dataF);

        File childrenF = new File(folder, "children.yml");
        if (!childrenF.exists()) childrenF.createNewFile();

        FileConfiguration children = YamlConfiguration.loadConfiguration(childrenF);
        children.set("children", this.children
                .stream()
                .map(Business::getUniqueId)
                .map(UUID::toString)
                .collect(Collectors.toList()));
        children.save(childrenF);
    }

    @NotNull
    private static Corporation readCorporation(File folder) throws IOException, IllegalStateException, ReflectiveOperationException {
        File info = new File(folder, "info.dat");
        if (!info.exists()) throw new IllegalStateException("Could not find: info.dat");

        ObjectInputStream infoIs = new ObjectInputStream(Files.newInputStream(info.toPath()));
        UUID id = (UUID) infoIs.readObject();
        long creationDate = infoIs.readLong();
        OfflinePlayer owner = Bukkit.getOfflinePlayer((UUID) infoIs.readObject());
        infoIs.close();

        Corporation c = new Corporation(id, creationDate, owner);

        File dataF = new File(folder, "data.yml");
        if (!dataF.exists()) throw new IllegalStateException("Could not find: data.yml");

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataF);
        c.name = data.getString("name");
        c.experience = data.getDouble("experience");
        c.icon = Material.valueOf(data.getString("icon"));
        c.headquarters = (Location) data.get("headquarters");

        File childrenF = new File(folder, "children.yml");
        if (!childrenF.exists()) throw new IllegalStateException("Could not find: children.yml");

        FileConfiguration children = YamlConfiguration.loadConfiguration(childrenF);
        c.children.addAll(children.getStringList("children")
                .stream()
                .map(UUID::fromString)
                .map(Business::getById)
                .collect(Collectors.toList()));

        return c;
    }

}