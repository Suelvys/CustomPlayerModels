package com.tom.cpm.common;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.common.registry.ForgeRegistries;

import com.tom.cpl.block.entity.EntityType;
import com.tom.cpl.block.entity.EntityTypeHandler;

public class EntityTypeHandlerImpl extends EntityTypeHandler<Class<?>> {
	public static final EntityTypeHandlerImpl impl = new EntityTypeHandlerImpl();

	@Override
	public List<EntityType> listNativeEntries(String tag) {
		if(tag.charAt(0) == '#') {
			return Collections.emptyList();
		} else {
			Class<?> item = EntityList.NAME_TO_CLASS.get(tag);
			if (item != null) {
				return Collections.singletonList(wrap(item));
			}
		}
		return Collections.emptyList();
	}

	@Override
	public List<String> listNativeTags() {
		return Collections.emptyList();
	}

	@Override
	public EntityType emptyObject() {
		return null;
	}

	@Override
	public boolean isInTag(String tag, Class<?> state) {
		return getEntityId(state).equals(tag);
	}

	@Override
	public List<String> listTags(Class<?> state) {
		return Collections.emptyList();
	}

	@Override
	public List<EntityType> getAllElements() {
		return EntityList.CLASS_TO_NAME.keySet().stream().map(this::wrap).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Class<?> a, Class<?> b) {
		return a == b;
	}

	@Override
	public String getEntityId(Class<?> state) {
		return EntityList.CLASS_TO_NAME.get(state);
	}

	@Override
	public List<String> listAllActiveEffectTypes() {
		return ForgeRegistries.POTIONS.getKeys().stream().map(ResourceLocation::toString).collect(Collectors.toList());
	}
}
