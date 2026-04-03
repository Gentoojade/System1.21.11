package dlindustries.vigillant.system.module;

import dlindustries.vigillant.system.utils.EncryptedString;

public enum Category {
	sword(EncryptedString.of("Sword")),
	CRYSTAL(EncryptedString.of("Crystal")),
	pot(EncryptedString.of("Potions")),
	mace(EncryptedString.of("SpearMace")),
	optimizer(EncryptedString.of("Utilities")),
	RENDER(EncryptedString.of("Render")),
	ESP(EncryptedString.of("Esp and Visuals")),
	CLIENT(EncryptedString.of("Client"));
	public final CharSequence name;

	Category(CharSequence name) {
		this.name = name;
	}
}
