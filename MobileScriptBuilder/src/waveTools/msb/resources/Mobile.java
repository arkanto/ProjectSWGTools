package waveTools.msb.resources;

import java.util.Vector;

public class Mobile {
	
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;
	
	private String scriptLocation;
	private String creatureName;
	private String defaultAttack;
	private int level;
	private int minLevel;
	private int maxLevel;
	private int difficulty;
	private int attackRange;
	private int weaponType;
	private float attackSpeed;
	private boolean deathblowEnabled;
	private Vector<String> creatureTemplates = new Vector<String>();
	private Vector<Weapon> weaponTemplates = new Vector<Weapon>();
	private Vector<String> attacks = new Vector<String>();
	private boolean dirty;
	
	public Mobile(String creatureName, String scriptLocation) { 
		this.creatureName = creatureName;
		this.scriptLocation = scriptLocation;
	}


	public String getScriptLocation() {
		return scriptLocation;
	}


	public void setScriptLocation(String scriptLocation) {
		this.scriptLocation = scriptLocation;
	}


	public String getCreatureName() {
		return creatureName;
	}


	public void setCreatureName(String creatureName) {
		this.creatureName = creatureName;
	}


	public Vector<String> getCreatureTemplates() {
		return creatureTemplates;
	}


	public void setCreatureTemplates(Vector<String> creatureTemplates) {
		this.creatureTemplates = creatureTemplates;
	}


	public Vector<Weapon> getWeaponTemplates() {
		return weaponTemplates;
	}


	public Vector<String> getAttacks() {
		return attacks;
	}


	public void setWeaponTemplates(Vector<Weapon> weaponTemplates) {
		this.weaponTemplates = weaponTemplates;
	}
	
	public int getLevel() {
		return level;
	}


	public void setLevel(int level) {
		this.level = level;
	}


	public int getMinLevel() {
		return minLevel;
	}


	public void setMinLevel(int minLevel) {
		this.minLevel = minLevel;
	}


	public int getMaxLevel() {
		return maxLevel;
	}


	public void setMaxLevel(int maxLevel) {
		this.maxLevel = maxLevel;
	}


	public float getAttackSpeed() {
		return attackSpeed;
	}


	public void setAttackSpeed(float attackSpeed) {
		this.attackSpeed = attackSpeed;
	}


	public int getAttackRange() {
		return attackRange;
	}


	public void setAttackRange(int attackRange) {
		this.attackRange = attackRange;
	}


	public int getWeaponType() {
		return weaponType;
	}


	public void setWeaponType(int weaponType) {
		this.weaponType = weaponType;
	}


	public int getDifficulty() {
		return difficulty;
	}


	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}


	public String getDefaultAttack() {
		return defaultAttack;
	}


	public void setDefaultAttack(String defaultAttack) {
		this.defaultAttack = defaultAttack;
	}


	public boolean isDirty() {
		return dirty;
	}


	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	public boolean isDeathblowEnabled() {
		return deathblowEnabled;
	}


	public void setDeathblowEnabled(boolean deathblowEnabled) {
		this.deathblowEnabled = deathblowEnabled;
	}


	public void addCreatureTemplate(String template) {
		creatureTemplates.add(template);
	}
	
	public void addAttack(String attack) {
		attacks.add(attack);
	}
	@Override
	public String toString() {
		if (dirty)
			return  "*" + creatureName;
		else return creatureName;
	}
}
