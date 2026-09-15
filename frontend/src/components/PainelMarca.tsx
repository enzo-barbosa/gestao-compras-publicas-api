interface ItemBullet {
  titulo: string
  texto: string
}

interface PainelMarcaProps {
  titulo: string
  bullets: ItemBullet[]
}

export default function PainelMarca({ titulo, bullets }: PainelMarcaProps) {
  return (
    <aside className="painel-marca" aria-label="Sobre o sistema">
      <span className="eyebrow">Gestão de compras públicas</span>
      <h1>{titulo}</h1>
      <ul className="bullets">
        {bullets.map((item) => (
          <li key={item.titulo} className="bullet">
            <span className="bullet-icone" aria-hidden="true">✓</span>
            <span>
              <strong>{item.titulo}</strong>
              {item.texto}
            </span>
          </li>
        ))}
      </ul>
    </aside>
  )
}