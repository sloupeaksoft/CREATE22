package com.rustcrates.block;

import com.rustcrates.AdminUtil;
import com.rustcrates.CrateTier;
import com.rustcrates.GuiHandler;
import com.rustcrates.RustCrates;
import com.rustcrates.data.CrateData;
import com.rustcrates.network.PacketHandler;
import com.rustcrates.tile.TileCrate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

@SuppressWarnings("deprecation")
public class BlockCrate extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    private static final double P = 1.0 / 16.0;

    public final CrateTier tier;
    public final boolean admin;
    private final AxisAlignedBB boxNS;
    private final AxisAlignedBB boxEW;

    public BlockCrate(CrateTier tier, boolean admin) {
        super(tier == CrateTier.NORMAL ? Material.WOOD : Material.IRON);
        this.tier = tier;
        this.admin = admin;
        String name = (admin ? "admin_" : "") + tier.id + "_crate";
        setRegistryName(RustCrates.MODID, name);
        setUnlocalizedName(RustCrates.MODID + "." + name);
        setSoundType(tier == CrateTier.NORMAL ? SoundType.WOOD : SoundType.METAL);
        setBlockUnbreakable();
        setResistance(6000000.0F);
        setCreativeTab(RustCrates.TAB);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        switch (tier) {
            case ELITE:
                boxNS = new AxisAlignedBB(0.5 * P, 0, 1.5 * P, 15.5 * P, 10 * P, 14.5 * P);
                boxEW = new AxisAlignedBB(1.5 * P, 0, 0.5 * P, 14.5 * P, 10 * P, 15.5 * P);
                break;
            case MILITARY:
                boxNS = new AxisAlignedBB(0, 0, 2.5 * P, 1, 10 * P, 13.5 * P);
                boxEW = new AxisAlignedBB(2.5 * P, 0, 0, 13.5 * P, 10 * P, 1);
                break;
            default:
                boxNS = FULL_BLOCK_AABB;
                boxEW = FULL_BLOCK_AABB;
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return true;
        if (world.isRemote) return true;
        if (admin) {
            if (!AdminUtil.isAdmin(player)) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "Этот ящик может открыть только администратор."));
                return true;
            }
            if (player.isSneaking()) {
                CrateData.Group g = CrateData.get().getOrCreateGroup(tier, world.provider.getDimension(), pos);
                PacketHandler.sendPanel((EntityPlayerMP) player, g, true);
            } else {
                player.openGui(RustCrates.instance, GuiHandler.GUI_POOL, world, pos.getX(), pos.getY(), pos.getZ());
            }
        } else {
            player.openGui(RustCrates.instance, GuiHandler.GUI_CRATE, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        if (!world.isRemote) register(world, pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (world.isRemote) return;
        register(world, pos);
        if (admin && placer instanceof EntityPlayer) {
            placer.sendMessage(new TextComponentString(TextFormatting.GREEN + "[ADMIN] Новые ящики типа «" + tier.title
                    + "» теперь привязываются к этому ADMIN-ящику. Предыдущие ADMIN-ящики закреплены."));
        }
    }

    /** Регистрация поставленного ящика (безопасно вызывать повторно). */
    private void register(World world, BlockPos pos) {
        CrateData data = CrateData.get();
        int dim = world.provider.getDimension();
        if (admin) data.onAdminPlaced(tier, dim, pos);
        else data.addCrate(tier, dim, pos);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            if (admin) CrateData.get().onAdminRemoved(world.provider.getDimension(), pos);
            else CrateData.get().removeCrate(world.provider.getDimension(), pos);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return !admin;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return admin ? null : new TileCrate();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        EnumFacing f = state.getValue(FACING);
        return (f == EnumFacing.NORTH || f == EnumFacing.SOUTH) ? boxNS : boxEW;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return tier == CrateTier.NORMAL;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return tier == CrateTier.NORMAL;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return tier == CrateTier.NORMAL ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }
}
