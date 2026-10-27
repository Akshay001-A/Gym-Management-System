import { Eye, Pencil, Trash2 } from 'lucide-react'

export default function MemberTable({ members = [], compact = false, onView, onEdit, onDelete }) {
  if (members.length === 0) {
    return (
      <div className="table-empty">
        <strong>No members found</strong>
        <p>Try another search or add a new member.</p>
      </div>
    );
  }

  return (
    <div className={`table-wrap ${compact ? 'compact-member-wrap' : 'member-directory-wrap'}`}>
      <table className={`member-table ${compact ? 'compact-member-table' : 'member-directory-table'}`}>
        <thead>
          <tr>
            <th scope="col">MEMBER</th>
            <th scope="col">MEMBER ID</th>
            {!compact && <><th scope="col">AGE</th><th scope="col">MOBILE</th><th scope="col">AMOUNT</th></>}
            {!compact && onView && <th scope="col">ACTIONS</th>}
          </tr>
        </thead>

        <tbody>
          {members.map((member) => (
            <tr key={member.id}>
              <td data-label="Member">
                <div className="member-cell">
                  <span className="member-avatar" aria-hidden="true">
                    {member.name?.trim().charAt(0).toUpperCase() || "M"}
                  </span>

                  <span className="member-identity">
                    <strong>{member.name || "Unnamed member"}</strong>
                    <small>{member.email || "No email provided"}</small>
                  </span>
                </div>
              </td>

              <td className="id-cell" data-label="Member ID">
                {typeof member.id === "number" ? `#${member.id}` : member.id}
              </td>

              {!compact && <>
                <td data-label="Age">{member.age ?? '—'}</td>
                <td data-label="Mobile">{member.mobile || '—'}</td>
                <td data-label="Amount">{member.amount ?? '—'}</td>
              </>}
              {!compact && onView && <td data-label="Actions"><div className="row-actions">
                <button type="button" aria-label={`View ${member.name}`} title="View details" onClick={() => onView(member)}><Eye size={15} /></button>
                <button type="button" aria-label={`Edit ${member.name}`} title="Edit member" onClick={() => onEdit(member)}><Pencil size={15} /></button>
                <button type="button" aria-label={`Delete ${member.name}`} title="Delete member" onClick={() => onDelete(member)}><Trash2 size={15} /></button>
              </div></td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
